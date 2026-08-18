package com.ject.vs.home.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 핫토픽 카드를 클릭했음을 알리는 요청. voteId는 경로에서 받는다.
 *
 * <p>노출 영역(캐러셀/리스트)은 보내지 않는다. 서버가 rank로 판정한다 —
 * 이유는 {@link com.ject.vs.home.domain.HotTopicArea} 참고.
 */
public record HotTopicClickRequest(

        @Schema(description = "클릭한 카드의 핫토픽 순위. 핫토픽 조회 응답의 rank를 그대로 돌려주세요. "
                + "1~3은 상단 캐러셀, 4~5는 하단 리스트로 집계됩니다.",
                example = "1", minimum = "1", maximum = "5")
        @NotNull
        @Min(1)
        @Max(5)
        Integer rank
) {
}
