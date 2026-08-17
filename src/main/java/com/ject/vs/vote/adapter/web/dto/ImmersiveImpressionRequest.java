package com.ject.vs.vote.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 몰입형 투표 콘텐츠가 화면에 노출됐음을 알리는 요청. voteId는 경로에서 받는다.
 *
 * <p>이 요청 1건이 곧 지표 ①(투표 전환율)의 분모 1이고, ②·③의 시간 기준점이다.
 */
public record ImmersiveImpressionRequest(

        @Schema(description = "이 노출을 식별하는 클라이언트 생성 UUID. 같은 콘텐츠를 다시 보면 새 값을 발급한다.",
                example = "8f14e45f-ceea-467a-9f0b-1c1e0b2d3a4b")
        @NotBlank
        @Size(max = 64)
        String impressionId,

        @Schema(description = "피드 내 순서(0부터). 앞쪽 콘텐츠일수록 전환이 높은지 보기 위한 보조 지표.", example = "0")
        @PositiveOrZero
        Integer position
) {
}
