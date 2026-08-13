package com.ject.vs.home.port.in;

/**
 * 핫토픽 순위 캐시를 다시 계산해 갱신한다. 3시간 주기 스케줄러가 호출한다.
 */
public interface HotTopicRefreshUseCase {

    void refreshHotTopics();
}
