package com.ject.vs.config;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.auth.port.AuthService;
import com.ject.vs.auth.port.in.dto.LoginTokenResponse;
import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.user.domain.UtmAttribution;
import com.ject.vs.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuth2Properties oauth2Properties;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;
    private final AnalyticsEventLogger analytics;
    private final UtmCookie utmCookie;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        try {
            UtmAttribution utm = utmCookie.read(request);

            LoginTokenResponse loginResponse = authService.socialLogin(email, utm);

            addTokenCookies(response, loginResponse);

            String targetUrl = determineTargetUrl(loginResponse);

            AnalyticsEvent event = AnalyticsEvent.of("signup_completed")
                    .userId(loginResponse.getUserId())
                    .put("method", resolveMethod(authentication));
            if (!utm.isEmpty()) {
                event.put("utm_source", utm.source())
                        .put("utm_medium", utm.medium())
                        .put("utm_campaign", utm.campaign())
                        .put("utm_content", utm.content());
            }
            analytics.log(event);

            // 출처를 소비했으므로 쿠키를 만료시켜 다음 가입에 새 유입이 잡히도록 한다.
            utmCookie.clear(response);

            log.info("=== OAuth2 Login Success ===");
            log.info("email: {}", email);
            log.info("userStatus: {}, onboardingCompleted: {}",
                    loginResponse.getUserStatus(), loginResponse.isOnboardingCompleted());
            log.info("targetUrl: {}", targetUrl);

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("=== OAuth2 Login Error === email: {}", email, e);
            // 백엔드 기본 로그인 페이지(/login?error)로 보내면 사용자에겐 원인 불명의
            // "Invalid credentials"만 뜨고 프론트도 실패를 감지할 수 없다. 프론트로 되돌려
            // 에러 코드를 넘긴다.
            getRedirectStrategy().sendRedirect(request, response, loginFailureUrl());
        }
    }

    /**
     * 가입 절차를 마친 사용자만 홈으로 보내고, 나머지는 온보딩 페이지로 보낸다.
     *
     * <p>판정 기준은 userStatus가 아니라 실제 프로필 값의 유무다. 상태 enum을 갱신하지 않는
     * 경로가 있어 온보딩을 건너뛴 사용자가 REGISTER로 남아 있을 수 있고, 그 경우
     * 출생연도/성별이 영구히 공란이 된다(가입 이후 입력 수단이 없다).
     */
    private String determineTargetUrl(LoginTokenResponse loginResponse) {
        return loginResponse.isOnboardingCompleted()
                ? oauth2Properties.redirectSuccessUrl()
                : oauth2Properties.extraInfoUrl();
    }

    /** 로그인 실패 시 프론트로 되돌아갈 URL. 성공 URL에 error 쿼리 파라미터만 붙인다. */
    private String loginFailureUrl() {
        String base = oauth2Properties.redirectSuccessUrl();
        return base + (base.contains("?") ? "&" : "?") + "error=login_failed";
    }

    private void addTokenCookies(HttpServletResponse response, LoginTokenResponse loginResponse) {
        long accessTokenExpiration = jwtProperties.accessTokenExpirationSeconds();
        long refreshTokenExpiration = jwtProperties.refreshTokenExpirationSeconds();

        ResponseCookie accessTokenCookie = ResponseCookie.from(CookieUtil.CookieType.ACCESS_TOKEN, loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .domain(cookieProperties.domain())
                .maxAge(accessTokenExpiration)
                .sameSite(cookieProperties.sameSite())
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(CookieUtil.CookieType.REFRESH_TOKEN, loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/")
                .domain(cookieProperties.domain())
                .maxAge(refreshTokenExpiration)
                .sameSite(cookieProperties.sameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    /** 소셜 로그인 제공자(kakao/apple)를 method 값으로 사용. */
    private String resolveMethod(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId();
        }
        return null;
    }
}
