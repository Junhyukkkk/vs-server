package com.ject.vs.admin.analytics;

/**
 * 지표 계산 방식.
 */
public enum MetricKind {
    /** 이벤트 발생 건수를 센다. 대부분의 지표가 여기 속한다. */
    COUNT,

    /**
     * {@code immersive_vote_participated}의 {@code time_to_vote_ms} 평균("Time to Vote").
     * action=VOTED인 건만 대상이고, 항상 시안(variant)별로 쪼개진다 — 지표 자체의 정의라
     * 화면의 "쪼개서 보기" 선택과 무관하게 고정이다.
     */
    TIME_TO_VOTE_AVG,

    /**
     * {@code immersive_content_viewed}는 있는데 같은 impression_id의
     * {@code immersive_first_action}이 없는 노출("이탈"). 항상 시안(variant)별로 쪼개진다.
     */
    BOUNCE
}
