package com.ject.vs.home.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.config.AnonymousId;
import com.ject.vs.home.adapter.web.dto.HotTopicClickRequest;
import com.ject.vs.home.domain.HotTopicArea;
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
 * 홈 화면 영역별 클릭 수집 전용 엔드포인트.
 *
 * <p>핫토픽 TOP5(상단 캐러셀 1~3위 / 하단 리스트 4~5위)와 '모든 투표' 리스트 중 어느 영역이
 * 실제 투표 진입을 만드는지 비교하기 위한 지표다. 세 이벤트 모두 목적지는 같은 투표 상세 화면이라
 * 도착 지점만 봐서는 구분되지 않으므로, 출발한 영역을 클릭 시점에 남긴다.
 *
 * <p>{@link HomeController}는 화면이 요청한 김에 데이터를 돌려주지만 여기 두 엔드포인트는
 * 응답으로 돌려줄 데이터가 없고 오로지 로그를 남기려고 존재한다. 성격이 다르므로 파일을 나눴다.
 *
 * <p><b>{@code SecurityPaths}를 함께 손봐야 하는 이유.</b> {@code /api/home/**}는 원래 통째로
 * {@code PUBLIC_ENDPOINTS}(= JWT 제외 경로)라 {@code JwtAuthFilter}가 아예 돌지 않았다. 그 상태로
 * 여기에 클릭 수집을 붙이면 로그인 사용자의 클릭도 {@code user_id=null, is_member=false}로 적재돼
 * 회원/비회원 세그먼트가 전부 비회원으로 쏠린다. 그래서 조회용 GET 세 개만 제외 경로로 남기고,
 * 클릭 수집 경로는 {@code OPTIONAL_AUTH_ENDPOINTS}로 옮겨 인증 없이 열려 있으면서도 필터가 돌게 했다.
 * 몰입형 트래킹을 {@code /api/track} 대신 {@code /api/immersive-votes} 아래 둔 것과 같은 이유다.
 *
 * <p>두 엔드포인트 모두 본문 없이 204를 돌려준다. 클라이언트는 화면 전환을 막지 말고
 * fire-and-forget으로 호출하면 된다.
 */
@Tag(name = "홈 트래킹", description = "홈 화면 영역별 클릭 수집 API")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeTrackingController {

    private final AnalyticsEventLogger analytics;

    @Operation(summary = "핫토픽 카드 클릭 기록",
            description = "핫토픽 TOP5 카드를 눌러 투표 상세로 이동할 때 1회 호출합니다. "
                    + "rank에 따라 상단 캐러셀(1~3위)은 hot_topic_carousel_clicked, "
                    + "하단 리스트(4~5위)는 hot_topic_list_clicked로 나뉘어 적재됩니다.")
    @PostMapping("/hot-topics/{voteId}/click")
    public ResponseEntity<Void> logHotTopicClick(
            @PathVariable Long voteId,
            @RequestBody @Valid HotTopicClickRequest request,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {

        HotTopicArea area = HotTopicArea.ofRank(request.rank());

        analytics.log(AnalyticsEvent.of(area.eventName())
                .anonymousId(anonymousId)
                .put("vote_id", voteId)
                .put("rank", request.rank()));

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "모든 투표 리스트 클릭 기록",
            description = "'모든 투표' 영역의 카드를 눌러 투표 상세로 이동할 때 1회 호출합니다. "
                    + "핫토픽 영역과 구분해 이 영역에서 발생한 클릭 총량을 재는 것이 목적이라 rank는 받지 않습니다.")
    @PostMapping("/votes/{voteId}/click")
    public ResponseEntity<Void> logAllVotesClick(
            @PathVariable Long voteId,
            @Parameter(hidden = true) @AnonymousId String anonymousId) {

        // 이 영역엔 순위 개념이 없다. 어떤 투표가 눌렸는지는 남겨 두면 인기 순위 산정과 대조할 수 있어 vote_id만 싣는다.
        analytics.log(AnalyticsEvent.of("all_votes_clicked")
                .anonymousId(anonymousId)
                .put("vote_id", voteId));

        return ResponseEntity.noContent().build();
    }
}
