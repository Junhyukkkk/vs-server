package com.ject.vs.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 행동 로그 분석 관련 설정.
 *
 * <p>{@code excludedAnonymousIds}는 분석 집계에서 빼고 볼 {@code anonymous_id} 목록이다.
 * 내부 QA 기기처럼 한 사람이 같은 시안을 수백 번 찍어 특정 시안의 합계를 부풀리는 경우,
 * 이 목록에 넣으면 "지정 ID 제외" 지표가 그 사용자를 걸러낸다.
 *
 * <p>환경변수 {@code ANALYTICS_EXCLUDED_ANONYMOUS_IDS}에 콤마로 구분해 지정한다.
 * 코드 수정·재배포 없이 테스터를 추가/제거할 수 있도록 설정으로 뺐다.
 */
@ConfigurationProperties(prefix = "analytics")
public record AnalyticsProperties(
        List<String> excludedAnonymousIds
) {

    public AnalyticsProperties {
        excludedAnonymousIds = excludedAnonymousIds == null ? List.of() : List.copyOf(excludedAnonymousIds);
    }

    /** 네이티브 쿼리 파라미터로 넘길 콤마 결합 문자열. 목록이 비면 빈 문자열. */
    public String excludedAnonymousIdsCsv() {
        return String.join(",", excludedAnonymousIds);
    }
}
