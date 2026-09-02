package com.ject.vs.admin.analytics;

import com.ject.vs.analytics.AnalyticsEventRepository;
import com.ject.vs.analytics.AnalyticsEventRepository.DailyAvgRow;
import com.ject.vs.analytics.AnalyticsEventRepository.DailyBounceRow;
import com.ject.vs.analytics.AnalyticsEventRepository.DailyCountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 어드민 분석 화면(/admin/analytics)의 집계 서비스. 선택된 지표마다
 * {@link AnalyticsCatalog}의 {@link MetricKind}에 따라 알맞은 저장소 쿼리를 호출하고,
 * 화면이 바로 뿌릴 수 있는 표·그래프 형태({@link MetricResult})로 조립한다.
 */
@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd(E)", Locale.KOREAN);

    /** 조회 기간 상한. 관리자가 실수로 "전체 기간"을 눌러도 매 요청이 과도한 스캔을 하지 않도록 막는다. */
    private static final int MAX_RANGE_DAYS = 92;

    private final AnalyticsEventRepository analyticsEventRepository;

    public Map<String, List<AnalyticsMetricDef>> catalogGroups() {
        return AnalyticsCatalog.byGroup();
    }

    public List<AnalyticsBreakdown> breakdownOptions() {
        return AnalyticsCatalog.allBreakdowns();
    }

    /** from이 to보다 늦거나 기간이 상한을 넘으면 to를 기준으로 안전한 범위로 되돌린다. */
    public LocalDate[] clampRange(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from.isAfter(to) ? to : from;
        if (ChronoUnit.DAYS.between(safeFrom, to) >= MAX_RANGE_DAYS) {
            safeFrom = to.minusDays(MAX_RANGE_DAYS - 1);
        }
        return new LocalDate[]{safeFrom, to};
    }

    /** 카탈로그에 없는 값은 조용히 걸러진다 — 지표를 추가/삭제해도 오래된 북마크 URL이 에러를 내지 않는다. */
    public String normalizeBreakdown(String requested) {
        if (requested == null) {
            return AnalyticsBreakdown.NONE.propertyKey();
        }
        return breakdownOptions().stream()
                .map(AnalyticsBreakdown::propertyKey)
                .filter(key -> key.equals(requested))
                .findFirst()
                .orElse(AnalyticsBreakdown.NONE.propertyKey());
    }

    @Transactional(readOnly = true)
    public AnalyticsQueryResult query(List<String> metricIds, LocalDate from, LocalDate to, String breakdownKey) {
        Instant fromUtc = from.atStartOfDay(KST).toInstant();
        Instant toUtcExclusive = to.plusDays(1).atStartOfDay(KST).toInstant();
        List<LocalDate> days = daysBetween(from, to);

        List<MetricResult> results = metricIds.stream()
                .distinct()
                .map(AnalyticsCatalog::find)
                .flatMap(Optional::stream)
                .map(def -> buildResult(def, days, fromUtc, toUtcExclusive, breakdownKey))
                .toList();

        return new AnalyticsQueryResult(from, to, results);
    }

    private MetricResult buildResult(AnalyticsMetricDef def, List<LocalDate> days,
                                      Instant fromUtc, Instant toUtc, String breakdownKey) {
        return switch (def.kind()) {
            case COUNT -> buildCountResult(def, days, fromUtc, toUtc, breakdownKey);
            case TIME_TO_VOTE_AVG -> buildTimeToVoteResult(def, days, fromUtc, toUtc);
            case BOUNCE -> buildBounceResult(def, days, fromUtc, toUtc);
        };
    }

    private MetricResult buildCountResult(AnalyticsMetricDef def, List<LocalDate> days,
                                           Instant fromUtc, Instant toUtc, String breakdownKey) {
        List<DailyCountRow> rows = analyticsEventRepository.aggregateCountDaily(
                def.eventName(), breakdownKey, fromUtc, toUtc);

        List<String> series = seriesOrDefault(rows.stream().map(DailyCountRow::getBucketValue));
        Map<String, Map<LocalDate, Double>> chartValues = new LinkedHashMap<>();
        for (DailyCountRow row : rows) {
            chartValues.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>())
                    .put(row.getBucketDay().toLocalDate(), row.getEventCount().doubleValue());
        }

        long total = rows.stream().mapToLong(DailyCountRow::getEventCount).sum();
        String summary = "총 %s건".formatted(format(total));

        return toResult(def, days, series, chartValues, summary,
                (day, s) -> format((long) chartValues.getOrDefault(s, Map.of()).getOrDefault(day, 0.0).doubleValue()));
    }

    private MetricResult buildTimeToVoteResult(AnalyticsMetricDef def, List<LocalDate> days,
                                                Instant fromUtc, Instant toUtc) {
        List<DailyAvgRow> rows = analyticsEventRepository.aggregateTimeToVoteDaily(fromUtc, toUtc);

        List<String> series = seriesOrDefault(rows.stream().map(DailyAvgRow::getBucketValue));
        Map<String, Map<LocalDate, Double>> chartValues = new LinkedHashMap<>();
        Map<String, Map<LocalDate, Long>> sampleBySeries = new LinkedHashMap<>();
        for (DailyAvgRow row : rows) {
            LocalDate day = row.getBucketDay().toLocalDate();
            chartValues.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>()).put(day, row.getAvgMillis() / 1000.0);
            sampleBySeries.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>()).put(day, row.getSampleCount());
        }

        long totalSamples = rows.stream().mapToLong(DailyAvgRow::getSampleCount).sum();
        double weightedMillis = rows.stream().mapToDouble(r -> r.getAvgMillis() * r.getSampleCount()).sum();
        String summary = totalSamples == 0
                ? "표본 없음"
                : "평균 %.1f초 (표본 %s건)".formatted(weightedMillis / totalSamples / 1000.0, format(totalSamples));

        return toResult(def, days, series, chartValues, summary, (day, s) -> {
            Long sample = sampleBySeries.getOrDefault(s, Map.of()).get(day);
            if (sample == null || sample == 0) {
                return "-";
            }
            double seconds = chartValues.get(s).get(day);
            return "%.1f초 (n=%s)".formatted(seconds, format(sample));
        });
    }

    private MetricResult buildBounceResult(AnalyticsMetricDef def, List<LocalDate> days,
                                            Instant fromUtc, Instant toUtc) {
        List<DailyBounceRow> rows = analyticsEventRepository.aggregateImmersiveBounceDaily(fromUtc, toUtc);

        List<String> series = seriesOrDefault(rows.stream().map(DailyBounceRow::getBucketValue));
        Map<String, Map<LocalDate, Double>> chartValues = new LinkedHashMap<>();
        Map<String, Map<LocalDate, Long>> viewedBySeries = new LinkedHashMap<>();
        Map<String, Map<LocalDate, Long>> bouncedBySeries = new LinkedHashMap<>();
        for (DailyBounceRow row : rows) {
            LocalDate day = row.getBucketDay().toLocalDate();
            chartValues.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>())
                    .put(day, row.getBouncedCount().doubleValue());
            viewedBySeries.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>()).put(day, row.getViewedCount());
            bouncedBySeries.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>()).put(day, row.getBouncedCount());
        }

        long totalViewed = rows.stream().mapToLong(DailyBounceRow::getViewedCount).sum();
        long totalBounced = rows.stream().mapToLong(DailyBounceRow::getBouncedCount).sum();
        String summary = totalViewed == 0
                ? "노출 없음"
                : "이탈 %s건 / 노출 %s건 (이탈률 %.1f%%)"
                        .formatted(format(totalBounced), format(totalViewed), totalBounced * 100.0 / totalViewed);

        return toResult(def, days, series, chartValues, summary, (day, s) -> {
            Long viewed = viewedBySeries.getOrDefault(s, Map.of()).get(day);
            if (viewed == null || viewed == 0) {
                return "-";
            }
            long bounced = bouncedBySeries.get(s).get(day);
            return "%s / %s (%.1f%%)".formatted(format(bounced), format(viewed), bounced * 100.0 / viewed);
        });
    }

    private MetricResult toResult(AnalyticsMetricDef def, List<LocalDate> days, List<String> series,
                                   Map<String, Map<LocalDate, Double>> chartValues, String summary,
                                   BiFunction<LocalDate, String, String> cellFormatter) {
        List<String> headers = new ArrayList<>();
        headers.add("날짜");
        headers.addAll(series);

        List<List<String>> tableRows = new ArrayList<>();
        for (LocalDate day : days) {
            List<String> row = new ArrayList<>();
            row.add(DATE_FMT.format(day));
            for (String s : series) {
                row.add(cellFormatter.apply(day, s));
            }
            tableRows.add(row);
        }

        return new MetricResult(def, summary, headers, tableRows,
                SvgChartRenderer.render(days, series, chartValues), SvgChartRenderer.legend(series));
    }

    /**
     * 데이터가 아예 없는 기간에도 표/그래프가 빈 화면 대신 0으로 채워진 "(전체)" 한 시리즈를 보여준다.
     * 이 라벨은 {@code AnalyticsEventRepository}의 세 집계 쿼리가 COALESCE로 채우는
     * {@code '(전체)'} 문자열과 반드시 같아야 한다 — SQL 문자열이라 Java 상수를 공유할 수 없어
     * 값만 맞춰 뒀다.
     */
    private static final String NO_BREAKDOWN_LABEL = "(전체)";

    private static List<String> seriesOrDefault(java.util.stream.Stream<String> valuesStream) {
        List<String> distinct = valuesStream.distinct().sorted(AdminAnalyticsService::compareBucket).toList();
        return distinct.isEmpty() ? List.of(NO_BREAKDOWN_LABEL) : distinct;
    }

    /** 순위(rank)처럼 숫자로 된 값은 숫자 순으로, 나머지는 문자열 순으로 정렬한다. */
    private static int compareBucket(String a, String b) {
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    private static List<LocalDate> daysBetween(LocalDate from, LocalDate to) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.add(d);
        }
        return days;
    }

    private static String format(long v) {
        return String.format(Locale.KOREA, "%,d", v);
    }
}
