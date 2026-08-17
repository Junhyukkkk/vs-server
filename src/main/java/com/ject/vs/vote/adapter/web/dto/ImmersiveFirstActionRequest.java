package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.ImmersiveFirstAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 한 노출에서 사용자가 가장 먼저 한 행동. 노출 1건당 최대 1회만 보낸다.
 *
 * <p>지표 ③(첫 행동 분포)의 원천이다. Time to Vote(②)는 이 이벤트가 아니라
 * {@code participate} 요청에서 받는다 — 첫 행동이 채팅이었다가 나중에 투표한 경우도
 * "노출 → 투표"에 포함돼야 하기 때문이다.
 */
public record ImmersiveFirstActionRequest(

        @Schema(description = "직전에 보낸 impression 요청과 같은 값. 이 값으로 노출과 행동을 잇는다.",
                example = "8f14e45f-ceea-467a-9f0b-1c1e0b2d3a4b")
        @NotBlank
        @Size(max = 64)
        String impressionId,

        @Schema(description = "첫 행동 종류. '무인터랙션 이탈'은 이 요청을 보내지 않는 것으로 표현한다.",
                example = "VOTE")
        @NotNull
        ImmersiveFirstAction action,

        @Schema(description = "노출부터 이 행동까지 걸린 시간(ms). performance.now() 차이로 잰다. "
                + "측정이 어려우면 생략 가능 — 분포(③) 집계에는 필요 없다.", example = "4200")
        Long elapsedMs
) {
}
