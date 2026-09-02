package com.ject.vs.admin.analytics;

/**
 * 지표를 값 기준으로 쪼개 볼 수 있는 속성 하나.
 *
 * <p>{@code propertyKey}는 {@code analytics_events.properties}(JSON) 안의 키 이름이고,
 * {@code label}은 "쪼개서 보기" 드롭다운에 표시되는 한글 이름이다.
 */
public record AnalyticsBreakdown(String propertyKey, String label) {

    /** "쪼개지 않음"을 뜻하는 값. 실제로 존재하지 않는 속성 키를 조회해 항상 하나의 묶음으로 합쳐진다. */
    public static final AnalyticsBreakdown NONE = new AnalyticsBreakdown("", "전체(안 쪼갬)");
}
