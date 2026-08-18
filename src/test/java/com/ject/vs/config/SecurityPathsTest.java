package com.ject.vs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 경로 목록의 "어느 쪽에 있느냐"가 로그의 신원 정확도를 좌우하는 지점을 고정한다.
 *
 * <p>{@code JWT_EXCLUDED_PATHS}에 걸리면 {@code JwtAuthFilter#shouldNotFilter}가 true가 되어
 * 필터가 통째로 건너뛴다. 그러면 로그인 사용자의 요청도 SecurityContext가 비어
 * {@code user_id=null, is_member=false}로 적재된다. 조회 API에는 무해하지만 클릭 수집에는 치명적이라,
 * 누군가 편의상 {@code /api/home/**}로 되돌리면 여기서 깨지게 둔다.
 */
@DisplayName("SecurityPaths: JWT 필터 제외 경로")
class SecurityPathsTest {

    private final AntPathMatcher matcher = new AntPathMatcher();

    private boolean jwtExcluded(String path) {
        return SecurityPaths.JWT_EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> matcher.match(pattern, path));
    }

    private boolean permitted(String path) {
        return SecurityPaths.PUBLIC_ENDPOINTS.stream().anyMatch(p -> matcher.match(p, path))
                || SecurityPaths.OPTIONAL_AUTH_ENDPOINTS.stream().anyMatch(p -> matcher.match(p, path));
    }

    @Test
    @DisplayName("홈 조회 API는 JWT 파싱 없이 통과한다")
    void 홈_조회는_필터를_건너뛴다() {
        assertThat(jwtExcluded("/api/home/recommendations")).isTrue();
        assertThat(jwtExcluded("/api/home/hot-topics")).isTrue();
        assertThat(jwtExcluded("/api/home/votes")).isTrue();
    }

    @Test
    @DisplayName("홈 클릭 수집은 필터를 타야 한다 - 건너뛰면 로그인 사용자도 전부 비회원으로 적재된다")
    void 홈_클릭_수집은_필터를_탄다() {
        assertThat(jwtExcluded("/api/home/hot-topics/77/click")).isFalse();
        assertThat(jwtExcluded("/api/home/votes/42/click")).isFalse();
    }

    @Test
    @DisplayName("홈 클릭 수집은 필터를 타면서도 비로그인 접근이 열려 있다")
    void 홈_클릭_수집은_인증_없이도_열려_있다() {
        assertThat(permitted("/api/home/hot-topics/77/click")).isTrue();
        assertThat(permitted("/api/home/votes/42/click")).isTrue();
    }
}
