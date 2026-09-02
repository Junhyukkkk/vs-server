package com.ject.vs.admin.port;

import com.ject.vs.config.AdminProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 어드민 화면 접근 권한 판정. {@code admin.user-ids}(ADMIN_USER_IDS 환경변수)에 등록된
 * userId만 어드민이다. 인증 자체는 서비스와 동일한 access_token 쿠키를 그대로 쓰고,
 * 여기서는 "로그인한 사람이 관리자인가"만 본다.
 *
 * <p>어드민 페이지(투표 생성, 이벤트 분석 등)가 늘어나도 판정 로직은 하나로 유지한다.
 */
@Component
@RequiredArgsConstructor
public class AdminAuthorizer {

    private final AdminProperties adminProperties;

    public boolean isAdmin(Long userId) {
        return userId != null
                && adminProperties.userIds() != null
                && adminProperties.userIds().contains(userId);
    }
}
