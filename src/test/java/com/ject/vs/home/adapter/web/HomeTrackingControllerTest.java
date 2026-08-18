package com.ject.vs.home.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.analytics.LoggedAnalyticsEvent;
import com.ject.vs.config.AnonymousIdResolver;
import com.ject.vs.config.CookieProperties;
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
 * 홈 화면 영역별 클릭 수집 엔드포인트가 "어떤 이름의 이벤트에 어떤 속성을 싣는지"를 고정한다.
 *
 * <p>이 이름들이 곧 GA4·Amplitude 대시보드의 지표 이름이자 분석 쿼리 규격이라, 무심코 바뀌면
 * 대시보드가 조용히 빈 값을 그린다. 그래서 응답 코드보다 적재된 이벤트 내용을 주로 검증한다.
 *
 * <p>{@code ImmersiveTrackingControllerTest}와 같은 이유로 standalone MockMvc를 쓴다.
 */
@DisplayName("HomeTrackingController: 홈 화면 영역별 클릭 수집")
class HomeTrackingControllerTest {

    private static final String ANONYMOUS_ID = "11111111-2222-3333-4444-555555555555";

    private MockMvc mockMvc;
    private AnalyticsEventLogger analytics;

    @BeforeEach
    void setUp() {
        analytics = mock(AnalyticsEventLogger.class);
        HomeTrackingController controller = new HomeTrackingController(analytics);

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
    @DisplayName("1~3위 클릭은 캐러셀 이벤트로 적재한다")
    void 캐러셀_클릭을_기록한다() throws Exception {
        mockMvc.perform(post("/api/home/hot-topics/{voteId}/click", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rank": 2}
                                """))
                .andExpect(status().isNoContent());

        assertThat(capturedEvent("hot_topic_carousel_clicked"))
                .containsEntry("vote_id", 77L)
                .containsEntry("rank", 2);
    }

    @Test
    @DisplayName("4~5위 클릭은 리스트 이벤트로 적재한다")
    void 리스트_클릭을_기록한다() throws Exception {
        mockMvc.perform(post("/api/home/hot-topics/{voteId}/click", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rank": 5}
                                """))
                .andExpect(status().isNoContent());

        assertThat(capturedEvent("hot_topic_list_clicked"))
                .containsEntry("vote_id", 77L)
                .containsEntry("rank", 5);
    }

    @Test
    @DisplayName("영역은 클라이언트가 아니라 서버가 rank로 정한다 - rank=5인 캐러셀 클릭 같은 모순된 로그가 생길 수 없다")
    void 영역은_서버가_rank로_정한다() throws Exception {
        mockMvc.perform(post("/api/home/hot-topics/{voteId}/click", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rank": 4, "area": "CAROUSEL"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(capturedEvent("hot_topic_list_clicked")).containsEntry("rank", 4);
    }

    @Test
    @DisplayName("TOP5 밖의 rank는 거절한다 - 화면에 없는 순위의 클릭은 지표를 오염시킨다")
    void 범위_밖_rank는_거절한다() throws Exception {
        mockMvc.perform(post("/api/home/hot-topics/{voteId}/click", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rank": 6}
                                """))
                .andExpect(status().isBadRequest());

        verify(analytics, never()).log(any());
    }

    @Test
    @DisplayName("rank가 없으면 거절한다 - 어느 영역 클릭인지 정할 수 없는 로그는 분석에 쓸 수 없다")
    void rank가_없으면_거절한다() throws Exception {
        mockMvc.perform(post("/api/home/hot-topics/{voteId}/click", 77L)
                        .cookie(anonymousCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(analytics, never()).log(any());
    }

    @Test
    @DisplayName("모든 투표 영역 클릭은 rank 없이 all_votes_clicked로 적재한다")
    void 모든_투표_클릭을_기록한다() throws Exception {
        mockMvc.perform(post("/api/home/votes/{voteId}/click", 42L)
                        .cookie(anonymousCookie()))
                .andExpect(status().isNoContent());

        Map<String, Object> properties = capturedEvent("all_votes_clicked");

        assertThat(properties).containsEntry("vote_id", 42L);
        assertThat(properties).doesNotContainKey("rank");
    }
}
