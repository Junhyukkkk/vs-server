package com.ject.vs.auth.port.in.dto;

import com.ject.vs.user.domain.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginTokenResponse {
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private UserStatus userStatus;

    /**
     * 회원가입 절차를 마쳤는지 여부. 로그인 후 홈으로 보낼지 온보딩 페이지로 보낼지를 가른다.
     * userStatus가 아니라 실제 프로필 값의 유무로 판정한다({@code User.hasCompletedOnboarding}).
     */
    private boolean onboardingCompleted;
}
