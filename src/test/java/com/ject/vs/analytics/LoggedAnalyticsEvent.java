package com.ject.vs.analytics;

import java.util.Map;

/**
 * 테스트에서 {@link AnalyticsEvent}의 내용을 들여다보기 위한 접근자.
 *
 * <p>{@code name()}/{@code properties()}는 패키지 전용이라 다른 패키지의 테스트에서 볼 수 없다.
 * 프로덕션 코드의 가시성을 테스트 때문에 넓히는 대신, 같은 패키지에 테스트 전용 클래스를 두어
 * 필요한 곳에서만 꺼내 쓴다.
 */
public final class LoggedAnalyticsEvent {

    private LoggedAnalyticsEvent() {
    }

    public static String nameOf(AnalyticsEvent event) {
        return event.name();
    }

    public static Map<String, Object> propertiesOf(AnalyticsEvent event) {
        return event.properties();
    }
}
