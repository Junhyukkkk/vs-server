package com.ject.vs.home.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * 핫토픽 인기 점수 계산.
 *
 * <p>인기 점수 = (참여 수 × 0.7) + (조회 수 × 0.3) + 신규 가중치
 *
 * <p>신규 가중치는 생성 직후 {@value #NEWNESS_BONUS_MAX}점에서 시작해 24시간에 걸쳐 선형으로 0까지 감쇠한다.
 * 갓 만들어진 투표에 노출 기회를 주면서, 점수를 연속값으로 만들어 동률이 사실상 발생하지 않게 한다.
 */
public final class HotTopicScorer {

    private static final double PARTICIPANT_WEIGHT = 0.7;
    private static final double VIEW_WEIGHT = 0.3;
    private static final double NEWNESS_BONUS_MAX = 5.0;
    private static final Duration NEWNESS_WINDOW = Duration.ofHours(24);

    private HotTopicScorer() {
    }

    public static double score(long participantCount, long viewCount, Instant createdAt, Instant now) {
        return engagementScore(participantCount, viewCount) + newnessBonus(createdAt, now);
    }

    private static double engagementScore(long participantCount, long viewCount) {
        return (participantCount * PARTICIPANT_WEIGHT) + (viewCount * VIEW_WEIGHT);
    }

    private static double newnessBonus(Instant createdAt, Instant now) {
        long windowMillis = NEWNESS_WINDOW.toMillis();
        long ageMillis = Duration.between(createdAt, now).toMillis();

        if (ageMillis >= windowMillis) {
            return 0.0;
        }
        // 시계 오차 등으로 생성 시각이 미래인 경우에도 최대 가중치를 넘지 않게 한다.
        if (ageMillis <= 0) {
            return NEWNESS_BONUS_MAX;
        }
        return NEWNESS_BONUS_MAX * (1.0 - ((double) ageMillis / windowMillis));
    }
}
