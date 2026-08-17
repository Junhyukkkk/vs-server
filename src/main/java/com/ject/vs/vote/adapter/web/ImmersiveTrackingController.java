package com.ject.vs.vote.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.analytics.ElapsedMillis;
import com.ject.vs.config.AnonymousId;
import com.ject.vs.experiment.AbTestAssigner;
import com.ject.vs.experiment.AbVariant;
import com.ject.vs.vote.adapter.web.dto.ImmersiveFirstActionRequest;
import com.ject.vs.vote.adapter.web.dto.ImmersiveImpressionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 몰입형 A/B 테스트 지표 수집 전용 엔드포인트.
 *
 * <p>기존 몰입형 API는 화면이 요청한 김에 로그를 곁들여 남기지만, 여기 두 엔드포인트는
 * 응답으로 돌려줄 데이터가 없고 오로지 로그를 남기려고 존재한다. 성격이 다르므로
 * {@link ImmersiveVoteController}와 파일을 나눴다.
 *
 * <p><b>경로를 {@code /api/track}이 아니라 {@code /api/immersive-votes} 아래 둔 이유.</b>
 * {@code /api/track/**}은 {@code SecurityPaths.PUBLIC_ENDPOINTS}(= JWT 제외 경로)라
 * {@code JwtAuthFilter}가 아예 돌지 않는다. 거기에 붙이면 로그인 사용자의 요청도
 * SecurityContext가 비어 {@code user_id=null, is_member=false}로 적재된다. 그러면 노출(분모)과
 * 투표(분자)의 신원 기준이 서로 달라져 회원/비회원 세그먼트 전환율이 어긋난다.
 * {@code /api/immersive-votes/**}는 {@code OPTIONAL_AUTH_ENDPOINTS}라 인증 없이 열려 있으면서도
 * 필터가 돌아 토큰이 있으면 userId를, 없으면 anonymous_id를 쓴다.
 *
 * <p>두 엔드포인트 모두 본문 없이 204를 돌려준다. 클라이언트는 fire-and-forget으로 호출하고
 * 응답을 기다리지 않아도 된다.
 */
@Tag(name = "몰입형 A/B 트래킹", description = "몰입형 투표 A/B 테스트 성과지표 수집 API")
@RestController
@RequestMapping("/api/immersive-votes")
@RequiredArgsConstructor
public class ImmersiveTrackingController {

    private final AnalyticsEventLogger analytics;
    private final AbTestAssigner abTestAssigner;

    /**
     * 시안은 클라이언트가 보내는 값을 믿지 않고 anonymous_id로 다시 계산한다.
     * 기존 몰입형 이벤트와 같은 방식이라 한 사용자의 노출·행동·투표에 늘 같은 시안이 실린다.
     */
    private AbVariant variantOf(String anonymousId) {
        return abTestAssigner.assign(AbTestAssigner.IMMERSIVE_UI, anonymousId);
    }

    @Operation(summary = "몰입형 콘텐츠 노출 기록",
            description = "투표 콘텐츠가 화면에 실제로 노출됐을 때 1회 호출합니다. "
                    + "이 호출 수가 투표 전환율의 분모이며, Time to Vote·첫 행동의 시간 기준점이 됩니다. "
                    + "impressionId는 노출마다 새로 발급해 이후 first-action·participate 호출에 같이 실어 주세요.")
    @PostMapping("/{voteId}/impression")
    public ResponseEntity<Void> logImpression(
            @PathVariable Long voteId,
            @RequestBody @Valid ImmersiveImpressionRequest request,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {

        analytics.log(AnalyticsEvent.of("immersive_content_viewed")
                .anonymousId(anonymousId)
                .put("vote_id", voteId)
                .put("impression_id", request.impressionId())
                .put("position", request.position())
                .put("variant", variantOf(anonymousId).name()));

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "몰입형 콘텐츠 첫 행동 기록",
            description = "해당 노출에서 사용자가 가장 먼저 한 행동을 1회만 호출합니다. 두 번째 행동부터는 보내지 않습니다. "
                    + "아무 인터랙션 없이 이탈한 경우는 호출하지 않습니다 — 노출은 있는데 이 호출이 없는 건을 이탈로 집계합니다.")
    @PostMapping("/{voteId}/first-action")
    public ResponseEntity<Void> logFirstAction(
            @PathVariable Long voteId,
            @RequestBody @Valid ImmersiveFirstActionRequest request,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {

        analytics.log(AnalyticsEvent.of("immersive_first_action")
                .anonymousId(anonymousId)
                .put("vote_id", voteId)
                .put("impression_id", request.impressionId())
                .put("action", request.action().name())
                .put("elapsed_ms", ElapsedMillis.normalize(request.elapsedMs()))
                .put("variant", variantOf(anonymousId).name()));

        return ResponseEntity.noContent().build();
    }
}
