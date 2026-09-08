package com.ject.vs.admin.analytics;

import com.ject.vs.analytics.AnalyticsEventRepository;
import com.ject.vs.analytics.AnalyticsEventRepository.VariantActionCountRow;
import com.ject.vs.analytics.AnalyticsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AdminAnalyticsService: 몰입형 첫 행동 분포 (시안별)")
class AdminAnalyticsServiceTest {

    private static final String METRIC_ID = "immersive_first_action_distribution";
    private static final String EXCLUDING_METRIC_ID = "immersive_first_action_distribution_excl";

    private AnalyticsEventRepository repository;
    private AdminAnalyticsService service;

    @BeforeEach
    void setUp() {
        repository = mock(AnalyticsEventRepository.class);
        service = serviceWithExclusions();
    }

    @Test
    @DisplayName("시안별로 첫 행동을 많은 순으로 나열하고, 각 시안 안에서의 비율을 매긴다")
    void 시안별_행동을_많은_순으로_나열한다() {
        when(repository.aggregateFirstActionDistribution(any(), any())).thenReturn(List.of(
                row("A", "VOTE", 12L),
                row("A", "CHAT", 6L),
                row("A", "SHARE", 2L),
                row("B", "EXPAND", 7L),
                row("B", "VOTE", 4L)
        ));

        MetricResult result = queryMetric(METRIC_ID);

        assertThat(result.tableHeaders()).containsExactly("순위", "A안", "B안");
        assertThat(result.tableRows().get(0)).containsExactly("1위", "투표 60% (12건)", "본문 펼쳐보기 64% (7건)");
        assertThat(result.tableRows().get(1)).containsExactly("2위", "채팅 30% (6건)", "투표 36% (4건)");
        assertThat(result.tableRows().get(2)).containsExactly("3위", "공유 10% (2건)", "-");
    }

    @Test
    @DisplayName("요약에 시안별 첫 행동 총건수를 표시한다")
    void 요약에_시안별_총건수를_표시한다() {
        when(repository.aggregateFirstActionDistribution(any(), any())).thenReturn(List.of(
                row("A", "VOTE", 12L),
                row("A", "CHAT", 6L),
                row("B", "VOTE", 4L)
        ));

        assertThat(queryMetric(METRIC_ID).summary()).isEqualTo("A안 18건 · B안 4건");
    }

    @Test
    @DisplayName("첫 행동 로그가 없으면 빈 표와 '첫 행동 없음' 요약을 낸다")
    void 첫_행동이_없으면_빈_결과다() {
        when(repository.aggregateFirstActionDistribution(any(), any())).thenReturn(List.of());

        MetricResult result = queryMetric(METRIC_ID);

        assertThat(result.summary()).isEqualTo("첫 행동 없음");
        assertThat(result.tableRows()).isEmpty();
    }

    @Test
    @DisplayName("A·B 외 시안 값은 열 맨 뒤에 두고, 모르는 행동 이름은 원래 값을 그대로 보여준다")
    void 미상_시안과_행동_처리() {
        when(repository.aggregateFirstActionDistribution(any(), any())).thenReturn(List.of(
                row("B", "VOTE", 3L),
                row("A", "VOTE", 5L),
                row("(미상)", "MYSTERY", 1L)
        ));

        MetricResult result = queryMetric(METRIC_ID);

        assertThat(result.tableHeaders()).containsExactly("순위", "A안", "B안", "(미상)");
        assertThat(result.tableRows().get(0))
                .containsExactly("1위", "투표 100% (5건)", "투표 100% (3건)", "MYSTERY 100% (1건)");
    }

    @Test
    @DisplayName("지정 ID 제외 지표는 설정된 anonymous_id를 콤마로 이어 쿼리에 넘긴다")
    void 제외_지표는_설정된_ID를_쿼리에_넘긴다() {
        when(repository.aggregateFirstActionDistributionExcluding(any(), any(), eq("tester-1,tester-2")))
                .thenReturn(List.of(row("A", "VOTE", 3L)));

        MetricResult result = queryMetric(EXCLUDING_METRIC_ID);

        assertThat(result.tableRows().get(0)).containsExactly("1위", "투표 100% (3건)");
        verify(repository).aggregateFirstActionDistributionExcluding(any(), any(), eq("tester-1,tester-2"));
        verify(repository, never()).aggregateFirstActionDistribution(any(), any());
    }

    @Test
    @DisplayName("지정 ID 제외 지표는 캡션에 제외한 ID 개수를 표시한다")
    void 제외_지표는_캡션에_제외_개수를_표시한다() {
        when(repository.aggregateFirstActionDistributionExcluding(any(), any(), any()))
                .thenReturn(List.of(row("A", "VOTE", 1L)));

        assertThat(queryMetric(EXCLUDING_METRIC_ID).breakdownCaption())
                .contains("지정 anonymous_id 2개 제외");
    }

    private AdminAnalyticsService serviceWithExclusions() {
        return new AdminAnalyticsService(repository, new AnalyticsProperties(List.of("tester-1", "tester-2")));
    }

    private MetricResult queryMetric(String metricId) {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 7, 23, 59);
        return service.query(List.of(metricId), from, to, "").metrics().get(0);
    }

    private static VariantActionCountRow row(String variant, String action, Long count) {
        return new VariantActionCountRow() {
            @Override
            public String getVariant() {
                return variant;
            }

            @Override
            public String getAction() {
                return action;
            }

            @Override
            public Long getEventCount() {
                return count;
            }
        };
    }
}
