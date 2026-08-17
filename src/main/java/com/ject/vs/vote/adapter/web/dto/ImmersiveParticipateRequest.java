package com.ject.vs.vote.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 몰입형 투표 참여/취소 요청.
 *
 * <p>일반 투표({@link ParticipateRequest})와 달리 A/B 지표용 노출 정보를 함께 받는다.
 * 일반 투표에는 "콘텐츠 노출" 개념이 없어 공유 DTO에 넣지 않고 몰입형 전용으로 분리했다.
 *
 * <p>노출 필드는 모두 선택값이다. 지표 수집이 빠지거나 실패해도 투표 자체는 반드시 성공해야 하므로,
 * 값이 없거나 이상해도 400을 내지 않고 해당 지표만 비운다.
 */
public record ImmersiveParticipateRequest(

        @Schema(description = "선택한 옵션 ID. 이미 선택한 옵션을 다시 보내면 투표가 취소된다.", example = "12")
        @NotNull
        Long optionId,

        @Schema(description = "이 투표가 일어난 노출의 impressionId. impression 호출 때 발급한 값을 그대로 보낸다.",
                example = "8f14e45f-ceea-467a-9f0b-1c1e0b2d3a4b")
        @Size(max = 64)
        String impressionId,

        @Schema(description = "콘텐츠 노출부터 이 투표까지 걸린 시간(ms) = Time to Vote. "
                + "performance.now() 차이로 잰다. 취소 요청일 때는 집계에서 제외되므로 보내지 않아도 된다.",
                example = "4200")
        Long elapsedMs
) {
}
