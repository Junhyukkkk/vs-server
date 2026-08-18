package com.ject.vs.home.domain;

/**
 * 핫토픽 TOP5가 홈 화면에서 노출되는 두 영역.
 *
 * <p>같은 핫토픽 목록이라도 1~3위는 상단 캐러셀로, 4~5위는 하단 리스트로 그려진다. 두 영역 중
 * 어느 쪽이 실제 유입을 만드는지 비교하는 것이 이 지표의 목적이므로 이벤트 이름부터 분리한다.
 *
 * <p><b>영역을 클라이언트가 보내지 않고 rank로 판정하는 이유.</b> 영역과 순위는 화면 설계상
 * 항상 붙어 다니는데(1~3위=캐러셀, 4~5위=리스트), 둘을 각각 받으면 {@code rank=5}인
 * 캐러셀 클릭 같은 모순된 로그가 섞일 수 있다. 그러면 두 이벤트의 합이 전체 클릭과 맞지 않아
 * 영역별 비교 자체가 무의미해진다. 한쪽만 받아 나머지를 유도하면 그 불일치가 발생할 수 없다.
 *
 * <p>진행 중인 투표가 5개 미만이면 있는 개수만큼만 노출되지만(3개면 캐러셀만, 4개면 캐러셀 3 +
 * 리스트 1) 순위와 영역의 대응은 그대로라 별도 분기가 필요 없다.
 */
public enum HotTopicArea {

    /** 상단 캐러셀(1~3위). 순위 배지가 썸네일 위에 붙는 큰 카드. */
    CAROUSEL("hot_topic_carousel_clicked"),

    /** 하단 리스트(4~5위). 순위 배지가 카드 왼쪽에 붙는 작은 행. */
    LIST("hot_topic_list_clicked");

    /** 캐러셀에 들어가는 마지막 순위. */
    private static final int LAST_CAROUSEL_RANK = 3;

    /** 핫토픽 TOP5의 마지막 순위. 이보다 큰 순위는 화면에 존재하지 않는다. */
    private static final int LAST_RANK = 5;

    private final String eventName;

    HotTopicArea(String eventName) {
        this.eventName = eventName;
    }

    /** GA4·Amplitude 대시보드와 분석 쿼리가 참조하는 이벤트 이름. 바꾸면 기존 집계가 끊긴다. */
    public String eventName() {
        return eventName;
    }

    /**
     * 순위가 노출된 영역을 판정한다.
     *
     * @throws IllegalArgumentException 1~5위 밖이면. 화면에 없는 순위의 클릭은 성립하지 않으므로,
     *                                  조용히 한쪽 영역으로 몰아 담지 않고 거절한다.
     */
    public static HotTopicArea ofRank(int rank) {
        if (rank < 1 || rank > LAST_RANK) {
            throw new IllegalArgumentException(
                    "핫토픽 순위는 1~%d위여야 합니다: %d".formatted(LAST_RANK, rank));
        }
        return rank <= LAST_CAROUSEL_RANK ? CAROUSEL : LIST;
    }
}
