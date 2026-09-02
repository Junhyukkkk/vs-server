package com.ject.vs.admin.analytics;

import com.ject.vs.analytics.AnalyticsEventRepository;
import com.ject.vs.analytics.AnalyticsEventRepository.BucketAvgRow;
import com.ject.vs.analytics.AnalyticsEventRepository.BucketBounceRow;
import com.ject.vs.analytics.AnalyticsEventRepository.BucketCountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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
import java.util.Set;
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

    private static final DateTimeFormatter DAILY_TABLE_FMT = DateTimeFormatter.ofPattern("MM/dd(E)", Locale.KOREAN);
    private static final DateTimeFormatter HOURLY_TABLE_FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter DAILY_AXIS_FMT = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter HOURLY_AXIS_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter RANGE_LABEL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 조회 기간 상한. 관리자가 실수로 "전체 기간"을 눌러도 매 요청이 과도한 스캔을 하지 않도록 막는다. */
    private static final int MAX_RANGE_DAYS = 92;

    /** 조회 범위가 이 시간 이하면 시간 단위로, 넘으면 일 단위로 그린다. 시간 단위 최대 구간 수는 3일×24=72개로 자동 제한된다. */
    private static final long HOURLY_THRESHOLD_HOURS = 72;

    private static final String VARIANT_FORCED_CAPTION = "쪼개기 기준: 시안(A/B) — 이 지표는 항상 시안별로 나옵니다";

    private final AnalyticsEventRepository analyticsEventRepository;

    public Map<String, List<AnalyticsMetricDef>> catalogGroups() {
        return AnalyticsCatalog.byGroup();
    }

    public List<AnalyticsBreakdown> breakdownOptions() {
        return AnalyticsCatalog.allBreakdowns();
    }

    /** "쪼개서 보기" 드롭다운을 체크한 지표에 맞게 좁히는 자바스크립트에 넘길 데이터. */
    public Map<String, String> breakdownApplicability() {
        return AnalyticsCatalog.breakdownApplicability();
    }

    /** 선택된 지표가 하나라도 있는 그룹 이름들. 사이드바에서 그 그룹만 펼쳐서 보여준다. */
    public Set<String> groupsContainingAny(List<String> metricIds) {
        return AnalyticsCatalog.groupsContainingAny(metricIds);
    }

    /** from이 to보다 늦거나 기간이 상한을 넘으면 to를 기준으로 안전한 범위로 되돌린다. */
    public LocalDateTime[] clampRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime safeFrom = from.isAfter(to) ? to : from;
        if (ChronoUnit.DAYS.between(safeFrom, to) >= MAX_RANGE_DAYS) {
            safeFrom = to.minusDays(MAX_RANGE_DAYS - 1);
        }
        return new LocalDateTime[]{safeFrom, to};
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
    public AnalyticsQueryResult query(List<String> metricIds, LocalDateTime from, LocalDateTime to, String breakdownKey) {
        boolean hourly = Duration.between(from, to).toHours() <= HOURLY_THRESHOLD_HOURS;
        QueryContext ctx = new QueryContext(
                bucketsBetween(from, to, hourly),
                from.atZone(KST).toInstant(),
                to.atZone(KST).toInstant(),
                hourly ? "hour" : "day",
                hourly ? HOURLY_TABLE_FMT : DAILY_TABLE_FMT,
                hourly ? HOURLY_AXIS_FMT : DAILY_AXIS_FMT,
                hourly ? "시각" : "날짜"
        );

        List<MetricResult> results = metricIds.stream()
                .distinct()
                .map(AnalyticsCatalog::find)
                .flatMap(Optional::stream)
                .map(def -> buildResult(def, ctx, breakdownKey))
                .toList();

        String rangeLabel = "%s ~ %s (KST, %s 단위)".formatted(
                RANGE_LABEL_FMT.format(from), RANGE_LABEL_FMT.format(to), hourly ? "시간" : "일");

        return new AnalyticsQueryResult(from, to, rangeLabel, results);
    }

    /** 한 번의 조회에서 지표마다 반복해서 넘기는 값들을 묶은 내부 컨텍스트. */
    private record QueryContext(
            List<LocalDateTime> buckets, Instant fromUtc, Instant toUtc, String unit,
            DateTimeFormatter tableFmt, DateTimeFormatter axisFmt, String bucketHeader
    ) {
    }

    private MetricResult buildResult(AnalyticsMetricDef def, QueryContext ctx, String breakdownKey) {
        return switch (def.kind()) {
            case COUNT -> buildCountResult(def, ctx, breakdownKey);
            case TIME_TO_VOTE_AVG -> buildTimeToVoteResult(def, ctx);
            case BOUNCE -> buildBounceResult(def, ctx);
        };
    }

    private MetricResult buildCountResult(AnalyticsMetricDef def, QueryContext ctx, String breakdownKey) {
        List<BucketCountRow> rows = analyticsEventRepository.aggregateCount(
                def.eventName(), breakdownKey, ctx.unit(), ctx.fromUtc(), ctx.toUtc());

        List<String> series = seriesOrDefault(rows.stream().map(BucketCountRow::getBucketValue));
        Map<String, Map<LocalDateTime, Double>> chartValues = new LinkedHashMap<>();
        for (BucketCountRow row : rows) {
            chartValues.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>())
                    .put(row.getBucketTime(), row.getEventCount().doubleValue());
        }

        long total = rows.stream().mapToLong(BucketCountRow::getEventCount).sum();
        String summary = "총 %s건".formatted(format(total));

        return toResult(def, ctx, series, chartValues, summary, breakdownCaption(def, breakdownKey),
                (bucket, s) -> format((long) chartValues.getOrDefault(s, Map.of()).getOrDefault(bucket, 0.0).doubleValue()));
    }

    private MetricResult buildTimeToVoteResult(AnalyticsMetricDef def, QueryContext ctx) {
        List<BucketAvgRow> rows = analyticsEventRepository.aggregateTimeToVote(ctx.unit(), ctx.fromUtc(), ctx.toUtc());

        List<String> series = seriesOrDefault(rows.stream().map(BucketAvgRow::getBucketValue));
        Map<String, Map<LocalDateTime, Double>> chartValues = new LinkedHashMap<>();
        Map<String, Map<LocalDateTime, Long>> sampleBySeries = new LinkedHashMap<>();
        for (BucketAvgRow row : rows) {
            chartValues.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>())
                    .put(row.getBucketTime(), row.getAvgMillis() / 1000.0);
            sampleBySeries.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>()).put(row.getBucketTime(), row.getSampleCount());
        }

        long totalSamples = rows.stream().mapToLong(BucketAvgRow::getSampleCount).sum();
        double weightedMillis = rows.stream().mapToDouble(r -> r.getAvgMillis() * r.getSampleCount()).sum();
        String summary = totalSamples == 0
                ? "표본 없음"
                : "평균 %.1f초 (표본 %s건)".formatted(weightedMillis / totalSamples / 1000.0, format(totalSamples));

        return toResult(def, ctx, series, chartValues, summary, VARIANT_FORCED_CAPTION, (bucket, s) -> {
            Long sample = sampleBySeries.getOrDefault(s, Map.of()).get(bucket);
            if (sample == null || sample == 0) {
                return "-";
            }
            double seconds = chartValues.get(s).get(bucket);
            return "%.1f초 (n=%s)".formatted(seconds, format(sample));
        });
    }

    private MetricResult buildBounceResult(AnalyticsMetricDef def, QueryContext ctx) {
        List<BucketBounceRow> rows = analyticsEventRepository.aggregateImmersiveBounce(ctx.unit(), ctx.fromUtc(), ctx.toUtc());

        List<String> series = seriesOrDefault(rows.stream().map(BucketBounceRow::getBucketValue));
        Map<String, Map<LocalDateTime, Double>> chartValues = new LinkedHashMap<>();
        Map<String, Map<LocalDateTime, Long>> viewedBySeries = new LinkedHashMap<>();
        Map<String, Map<LocalDateTime, Long>> bouncedBySeries = new LinkedHashMap<>();
        for (BucketBounceRow row : rows) {
            chartValues.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>())
                    .put(row.getBucketTime(), row.getBouncedCount().doubleValue());
            viewedBySeries.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>()).put(row.getBucketTime(), row.getViewedCount());
            bouncedBySeries.computeIfAbsent(row.getBucketValue(), k -> new HashMap<>()).put(row.getBucketTime(), row.getBouncedCount());
        }

        long totalViewed = rows.stream().mapToLong(BucketBounceRow::getViewedCount).sum();
        long totalBounced = rows.stream().mapToLong(BucketBounceRow::getBouncedCount).sum();
        String summary = totalViewed == 0
                ? "노출 없음"
                : "이탈 %s건 / 노출 %s건 (이탈률 %.1f%%)"
                        .formatted(format(totalBounced), format(totalViewed), totalBounced * 100.0 / totalViewed);

        return toResult(def, ctx, series, chartValues, summary, VARIANT_FORCED_CAPTION, (bucket, s) -> {
            Long viewed = viewedBySeries.getOrDefault(s, Map.of()).get(bucket);
            if (viewed == null || viewed == 0) {
                return "-";
            }
            long bounced = bouncedBySeries.get(s).get(bucket);
            return "%s / %s (%.1f%%)".formatted(format(bounced), format(viewed), bounced * 100.0 / viewed);
        });
    }

    /**
     * 화면 위쪽의 전역 "쪼개서 보기" 선택이 이 지표에 실제로 적용됐는지를 한 줄로 알려준다.
     * 지표마다 어떤 속성을 쪼갤 수 있는지가 다른데(예: "시안(A/B)"은 몰입형 지표에만 있음) 전역
     * 드롭다운 하나로 여러 지표를 동시에 조회하면, 카드마다 그 선택이 먹혔는지 아닌지가 갈린다.
     * 이 문장이 그 차이를 카드 위에서 바로 알려준다.
     */
    private static String breakdownCaption(AnalyticsMetricDef def, String breakdownKey) {
        if (breakdownKey == null || breakdownKey.isEmpty()) {
            return "쪼개기 기준: " + AnalyticsBreakdown.NONE.label();
        }
        return def.breakdowns().stream()
                .filter(b -> b.propertyKey().equals(breakdownKey))
                .map(b -> "쪼개기 기준: " + b.label())
                .findFirst()
                .orElse("이 지표엔 그 쪼개기 기준이 없어요 → 전체로 표시");
    }

    private MetricResult toResult(AnalyticsMetricDef def, QueryContext ctx, List<String> series,
                                   Map<String, Map<LocalDateTime, Double>> chartValues, String summary, String breakdownCaption,
                                   BiFunction<LocalDateTime, String, String> cellFormatter) {
        List<String> headers = new ArrayList<>();
        headers.add(ctx.bucketHeader());
        headers.addAll(series);

        List<List<String>> tableRows = new ArrayList<>();
        for (LocalDateTime bucket : ctx.buckets()) {
            List<String> row = new ArrayList<>();
            row.add(ctx.tableFmt().format(bucket));
            for (String s : series) {
                row.add(cellFormatter.apply(bucket, s));
            }
            tableRows.add(row);
        }

        return new MetricResult(def, breakdownCaption, summary, headers, tableRows,
                SvgChartRenderer.render(ctx.buckets(), series, chartValues, ctx.axisFmt()), SvgChartRenderer.legend(series));
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

    /** from~to 구간을 시간 또는 일 단위로 끊어 x축에 올릴 시각 목록을 만든다. 양끝은 각 단위로 내림한다. */
    private static List<LocalDateTime> bucketsBetween(LocalDateTime from, LocalDateTime to, boolean hourly) {
        ChronoUnit unit = hourly ? ChronoUnit.HOURS : ChronoUnit.DAYS;
        LocalDateTime start = from.truncatedTo(unit);
        LocalDateTime end = to.truncatedTo(unit);

        List<LocalDateTime> buckets = new ArrayList<>();
        for (LocalDateTime cur = start; !cur.isAfter(end); cur = cur.plus(1, unit)) {
            buckets.add(cur);
        }
        return buckets;
    }

    private static String format(long v) {
        return String.format(Locale.KOREA, "%,d", v);
    }
}
