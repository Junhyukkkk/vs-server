package com.ject.vs.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 소셜 로그인 성공 후 프론트엔드로 돌려보낼 URL.
 *
 * <p>두 값이 비어 있으면 기동을 실패시킨다. 값이 비면 {@link OAuth2LoginSuccessHandler}가
 * 빈 문자열로 리다이렉트하게 되는데, 빈 상대 경로는 현재 요청 URI(=이미 소비된 code가 붙은
 * OAuth 콜백)로 해석되어 invalid_grant → /login?error 로 끝난다.
 * 특히 extra-info-url은 가입(UNREGISTER) 사용자만 타는 경로라 기존 회원 로그인은
 * 멀쩡해 보이고, 신규 가입과 재가입만 조용히 막힌다.
 */
@Validated
@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(
        @NotBlank String redirectSuccessUrl,
        @NotBlank String extraInfoUrl
) {}
