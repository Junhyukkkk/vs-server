package com.ject.vs.home.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HotTopicArea: 핫토픽 순위 → 노출 영역 판정")
class HotTopicAreaTest {

    @Test
    @DisplayName("1~3위는 캐러셀 영역이다")
    void 상위_세_개는_캐러셀이다() {
        assertThat(HotTopicArea.ofRank(1)).isEqualTo(HotTopicArea.CAROUSEL);
        assertThat(HotTopicArea.ofRank(2)).isEqualTo(HotTopicArea.CAROUSEL);
        assertThat(HotTopicArea.ofRank(3)).isEqualTo(HotTopicArea.CAROUSEL);
    }

    @Test
    @DisplayName("4~5위는 리스트 영역이다")
    void 하위_두_개는_리스트다() {
        assertThat(HotTopicArea.ofRank(4)).isEqualTo(HotTopicArea.LIST);
        assertThat(HotTopicArea.ofRank(5)).isEqualTo(HotTopicArea.LIST);
    }

    @Test
    @DisplayName("영역별로 서로 다른 이벤트 이름을 쓴다 - 두 영역의 클릭을 분리해서 비교하는 게 이 지표의 목적이다")
    void 영역마다_이벤트_이름이_다르다() {
        assertThat(HotTopicArea.CAROUSEL.eventName()).isEqualTo("hot_topic_carousel_clicked");
        assertThat(HotTopicArea.LIST.eventName()).isEqualTo("hot_topic_list_clicked");
    }

    @Test
    @DisplayName("TOP5 밖의 순위는 거절한다 - 화면에 없는 순위의 클릭은 성립하지 않는다")
    void 범위_밖_순위는_거절한다() {
        assertThatThrownBy(() -> HotTopicArea.ofRank(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HotTopicArea.ofRank(6)).isInstanceOf(IllegalArgumentException.class);
    }
}
