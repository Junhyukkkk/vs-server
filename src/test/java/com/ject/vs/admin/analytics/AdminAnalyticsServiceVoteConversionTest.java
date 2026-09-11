package com.ject.vs.admin.analytics;

import com.ject.vs.analytics.AnalyticsEventRepository;
import com.ject.vs.analytics.AnalyticsEventRepository.VariantConversionRow;
import com.ject.vs.analytics.AnalyticsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * "노출 이벤트 건수"가 아니라 "그 시안을 본 서로 다른 사람 수"를 분모로 삼는 전환율 지표.
 * 한 사람이 콘텐츠를 여러 번 봐도 분모가 늘지 않아야 한다 — 이게 이 지표를 만든 이유다.
 */
@DisplayName("AdminAnalyticsService: 몰입형 투표 전환율 (시안별, 사람 수 기준)")
class AdminAnalyticsServiceVoteConversionTest {

    private static final String METRIC_ID = "immersive_vote_conversion_by_person";

    private AnalyticsEventRepository repository;
    private AdminAnalyticsService service;

    @BeforeEach
    void setUp() {
        repository = mock(AnalyticsEventRepository.class);
        service = new AdminAnalyticsService(repository, new AnalyticsProperties(List.of()));
    }

    @Test
    @DisplayName("시안별로 '본 사람 수'를 분모, '투표한 사람 수'를 분자로 전환율을 계산한다")
    void 시안별_전환율을_사람_수_기준으로_계산한다() {
        when(repository.aggregateVoteConversionByPerson(any(), any())).thenReturn(List.of(
                row("A", 44L, 13L),
                row("B", 45L, 10L)
        ));

        MetricResult result = queryConversion();

        assertThat(result.tableHeaders()).containsExactly("시안", "본 사람", "투표한 사람", "전환율");
        assertThat(result.tableRows().get(0)).containsExactly("A안", "44", "13", "29.5%");
        assertThat(result.tableRows().get(1)).containsExactly("B안", "45", "10", "22.2%");
    }

    @Test
    @DisplayName("본 사람이 0명이면 0으로 나누지 않고 0.0%로 표시한다")
    void 본_사람이_0명이면_0퍼센트다() {
        when(repository.aggregateVoteConversionByPerson(any(), any())).thenReturn(List.of(row("A", 0L, 0L)));

        assertThat(queryConversion().tableRows().get(0)).containsExactly("A안", "0", "0", "0.0%");
    }

    @Test
    @DisplayName("요약에 시안별 전환율과 분자/분모를 함께 보여준다")
    void 요약에_전환율과_분자_분모를_함께_보여준다() {
        when(repository.aggregateVoteConversionByPerson(any(), any())).thenReturn(List.of(
                row("A", 44L, 13L),
                row("B", 45L, 10L)
        ));

        assertThat(queryConversion().summary()).isEqualTo("A안 29.5% (13/44) · B안 22.2% (10/45)");
    }

    @Test
    @DisplayName("노출 로그가 없으면 빈 표와 '노출 없음' 요약을 낸다")
    void 노출이_없으면_빈_결과다() {
        when(repository.aggregateVoteConversionByPerson(any(), any())).thenReturn(List.of());

        MetricResult result = queryConversion();

        assertThat(result.summary()).isEqualTo("노출 없음");
        assertThat(result.tableRows()).isEmpty();
    }

    @Test
    @DisplayName("A·B 외 시안 값은 행 맨 뒤에 둔다")
    void 미상_시안은_뒤에_둔다() {
        when(repository.aggregateVoteConversionByPerson(any(), any())).thenReturn(List.of(
                row("B", 3L, 1L),
                row("A", 5L, 2L),
                row("(미상)", 1L, 0L)
        ));

        MetricResult result = queryConversion();

        assertThat(result.tableRows())
                .extracting(r -> r.get(0))
                .containsExactly("A안", "B안", "(미상)");
    }

    private MetricResult queryConversion() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 7, 23, 59);
        return service.query(List.of(METRIC_ID), from, to, "").metrics().get(0);
    }

    private static VariantConversionRow row(String variant, Long viewerCount, Long voterCount) {
        return new VariantConversionRow() {
            @Override
            public String getVariant() {
                return variant;
            }

            @Override
            public Long getViewerCount() {
                return viewerCount;
            }

            @Override
            public Long getVoterCount() {
                return voterCount;
            }
        };
    }
}
