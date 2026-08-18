package com.ject.vs.config;

import java.util.ArrayList;
import java.util.List;

public class SecurityPaths {

    public static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/health/**",
            "/",
            "/error",
            "/ws/**",
            "/oauth2/authorization/**",
            "/login/oauth2/code/**",
            // 홈 조회 API는 로그인 여부와 무관하게 같은 결과라 JWT 파싱 없이 통과시킨다.
            // 와일드카드(/api/home/**) 대신 세 경로를 나열하는 이유는 아래 OPTIONAL_AUTH_ENDPOINTS 참고.
            "/api/home/recommendations",
            "/api/home/hot-topics",
            "/api/home/votes",
            "/api/track/**",
            // 어드민 로그인 안내 페이지. (실제 어드민 기능은 /admin/votes 이하이며 인증 + admin.user-ids 검사를 거친다)
            "/admin/login"
    );

    /**
     * 로그인 없이도 접근 가능하지만, 토큰이 있으면 userId를 읽어야 하는 엔드포인트.
     * PUBLIC_ENDPOINTS(= JWT_EXCLUDED_PATHS)와 달리 JwtAuthFilter는 계속 실행되어
     * 토큰이 있으면 userId, 없으면 anonymousId로 동작한다.
     */
    public static final List<String> OPTIONAL_AUTH_ENDPOINTS = List.of(
            "/api/immersive-votes",
            "/api/immersive-votes/**",
            // 홈 클릭 수집. 위 조회 API와 달리 누가 눌렀는지를 남겨야 해서 필터가 돌아야 한다.
            // PUBLIC_ENDPOINTS에 /api/home/**가 있으면 JWT_EXCLUDED_PATHS에 걸려 필터가 통째로 건너뛰고
            // 로그인 사용자의 클릭까지 user_id=null로 적재되므로, 그쪽은 조회 경로만 나열해 두었다.
            "/api/home/hot-topics/*/click",
            "/api/home/votes/*/click",
            "/api/me/free-votes",
            "/api/votes/*",
            "/api/votes/*/participate",
            "/api/votes/*/result",
            "/api/votes/*/share",
            "/api/votes/*/emoji"
    );

    public static final List<String> JWT_EXCLUDED_PATHS = createJwtExcludedPaths();

    private static List<String> createJwtExcludedPaths() {
        List<String> paths = new ArrayList<>(PUBLIC_ENDPOINTS);

        paths.add("/auth/reissue");

        return List.copyOf(paths);
    }
}
