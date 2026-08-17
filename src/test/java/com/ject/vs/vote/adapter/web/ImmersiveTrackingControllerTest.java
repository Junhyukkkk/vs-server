package com.ject.vs.vote.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.analytics.LoggedAnalyticsEvent;
import com.ject.vs.config.AnonymousIdResolver;
import com.ject.vs.config.CookieProperties;
import com.ject.vs.experiment.AbTestAssigner;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A/B 지표 수집 엔드포인트가 "어떤 이름의 이벤트에 어떤 속성을 싣는지"를 고정한다.
 *
 * <p>이 속성 이름들이 곧 분석 쿼리와 프론트 연동 규격이라, 무심코 바뀌면 대시보드가 조용히
 * 빈 값을 그린다. 그래서 응답 코드보다 적재된 이벤트 내용을 주로 검증한다.
 *
 * <p>Spring 컨텍스트를 띄우지 않고 standalone MockMvc를 쓴다. 전체 컨텍스트는 SecurityConfig가
 * OAuth2·JWT·CORS 빈을 연쇄로 요구해 이 테스트의 관심사와 무관한 설정에 묶이기 때문이다.
 */
@DisplayName("ImmersiveTrackingController: 몰입형 A/B 지표 수집")
class ImmersiveTrackingControllerTest {

    private static final String ANONYMOUS_ID = "11111111-2222-3333-4444-555555555555";
    private static final String IMPRESSION_ID = "8f14e45f-ceea-467a-9f0b-1c1e0b2d3a4b";

    private MockMvc mockMvc;
    private AnalyticsEventLogger analytics;

    @BeforeEach
    void setUp() {
        analytics = mock(AnalyticsEventLogger.class);
        ImmersiveTrackingController controller =
                new ImmersiveTrackingController(analytics, new AbTestAssigner());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new AnonymousIdResolver(new CookieProperties(false, "Lax", null)))
                .build();
    }

    private Map<String, Object> capturedEvent(String expectedName) {
        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analytics).log(captor.capture());
        AnalyticsEvent event = captor.getValue();
        assertThat(LoggedAnalyticsEvent.nameOf(event)).isEqualTo(expectedName);
        return LoggedAnalyticsEvent.propertiesOf(event);
    }

    private Cookie anonymousCookie() {
        return new Cookie("anonymous_id", ANONYMOUS_ID);
    }

    @Test
    @DisplayName("노출 기록 시 전환율 분모가 될 immersive_content_viewed를 적재한다")
    void 노출을_기록한다() throws Exception {
        mockMvc.perform(post("/api/immersive-votes/{voteId}/impression", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"impressionId": "%s", "position": 3}
                                """.formatted(IMPRESSION_ID)))
                .andExpect(status().isNoContent());

        Map<String, Object> properties = capturedEvent("immersive_content_viewed");

        assertThat(properties)
                .containsEntry("vote_id", 77L)
                .containsEntry("impression_id", IMPRESSION_ID)
                .containsEntry("position", 3);
        assertThat(properties.get("variant")).isIn("A", "B");
    }

    @Test
    @DisplayName("첫 행동 기록 시 행동 종류와 경과 시간을 적재한다")
    void 첫_행동을_기록한다() throws Exception {
        mockMvc.perform(post("/api/immersive-votes/{voteId}/first-action", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"impressionId": "%s", "action": "EMOJI", "elapsedMs": 4200}
                                """.formatted(IMPRESSION_ID)))
                .andExpect(status().isNoContent());

        Map<String, Object> properties = capturedEvent("immersive_first_action");

        assertThat(properties)
                .containsEntry("vote_id", 77L)
                .containsEntry("impression_id", IMPRESSION_ID)
                .containsEntry("action", "EMOJI")
                .containsEntry("elapsed_ms", 4200L);
    }

    @Test
    @DisplayName("경과 시간이 음수면 이벤트는 남기되 시간만 비운다 - 지표를 오염시키지 않는다")
    void 비정상_경과시간은_비운다() throws Exception {
        mockMvc.perform(post("/api/immersive-votes/{voteId}/first-action", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"impressionId": "%s", "action": "VOTE", "elapsedMs": -5}
                                """.formatted(IMPRESSION_ID)))
                .andExpect(status().isNoContent());

        Map<String, Object> properties = capturedEvent("immersive_first_action");

        assertThat(properties).containsEntry("action", "VOTE");
        assertThat(properties.get("elapsed_ms")).isNull();
    }

    @Test
    @DisplayName("impressionId가 없으면 거절한다 - 노출과 행동을 이을 수 없는 로그는 분석에 쓸 수 없다")
    void impressionId가_없으면_거절한다() throws Exception {
        mockMvc.perform(post("/api/immersive-votes/{voteId}/first-action", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "VOTE", "elapsedMs": 100}
                                """))
                .andExpect(status().isBadRequest());

        verify(analytics, never()).log(any());
    }

    @Test
    @DisplayName("시안은 클라이언트 입력과 무관하게 anonymous_id로 정해져 노출과 행동에 같은 값이 실린다")
    void 시안은_anonymous_id로_일관되게_정해진다() throws Exception {
        mockMvc.perform(post("/api/immersive-votes/{voteId}/impression", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"impressionId": "%s", "position": 0, "variant": "B"}
                                """.formatted(IMPRESSION_ID)))
                .andExpect(status().isNoContent());

        String expected = new AbTestAssigner().assign(AbTestAssigner.IMMERSIVE_UI, ANONYMOUS_ID).name();

        assertThat(capturedEvent("immersive_content_viewed")).containsEntry("variant", expected);
    }
}
