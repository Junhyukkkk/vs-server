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
    BOUNCE,

    /**
     * {@code immersive_first_action}을 시안(variant)별로 묶고, 각 시안 안에서 행동(action)을
     * 많은 순으로 나열한 분포. 조회 기간 전체를 한 번에 집계하므로 시간 추세 그래프가 없다.
     */
    FIRST_ACTION_DISTRIBUTION,

    /**
     * {@link #FIRST_ACTION_DISTRIBUTION}과 같되 {@code analytics.excluded-anonymous-ids}에
     * 등록된 anonymous_id(내부 QA 기기 등)를 뺀 분포.
     */
    FIRST_ACTION_DISTRIBUTION_EXCLUDING
}
