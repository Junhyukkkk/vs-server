package com.ject.vs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인증 쿠키(accessToken/refreshToken)의 발급 속성.
 *
 * <p>{@code domain}은 쿠키를 어느 도메인 범위로 구울지 결정한다. 백엔드(api.vs.io.kr)가 발급하는
 * 쿠키를 프론트 도메인(vs.io.kr, www.vs.io.kr)과 같은 사이트로 취급시키려면 상위 도메인
 * {@code .vs.io.kr}을 지정해야 한다. 비워두면 Domain 속성 없이(host-only) 발급되어
 * api.vs.io.kr 요청에만 실린다.
 *
 * <p>프론트를 백엔드와 다른 등록가능도메인(예: *.netlify.app)에서 서비스하면 여기에 무엇을 넣어도
 * 서드파티 쿠키가 되어 사파리/시크릿창에서 차단된다. 이 경우 해결책은 쿠키 설정이 아니라
 * 프론트를 vs.io.kr 커스텀 도메인으로 서비스하는 것이다.
 */
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        boolean secure,
        String sameSite,
        String domain
) {
    public CookieProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "None";
        }
        // 빈 문자열을 그대로 넘기면 ResponseCookie가 Domain 속성을 생략하긴 하지만,
        // null로 정규화해 "미설정 = host-only"라는 의미를 코드에서 분명히 한다.
        if (domain != null && domain.isBlank()) {
            domain = null;
        }
    }
}
