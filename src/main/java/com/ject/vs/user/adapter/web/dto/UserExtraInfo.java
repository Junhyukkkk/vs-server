package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.ImageColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Year;

/**
 * 회원가입 절차에서 받는 정보. 네 값이 모두 채워져야 가입 완료로 처리한다.
 *
 * <p>출생연도와 성별은 가입 이후 수정할 수단이 없으므로(마이페이지는 닉네임/색상만 변경한다)
 * 하나라도 비어 있으면 저장하지 않고 거절한다. 비어 있는 채로 저장되면 영구히 공란으로 남는다.
 */
public record UserExtraInfo (
    @NotNull(message = "출생연도는 필수입니다.")
    Year birthDate,

    @NotNull(message = "성별은 필수입니다.")
    Gender gender,

    @NotBlank(message = "닉네임은 필수입니다.")
    String nickName,

    @NotNull(message = "프로필 색상은 필수입니다.")
    ImageColor imageColor
) {}
