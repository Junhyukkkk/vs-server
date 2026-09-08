package com.ject.vs.admin.analytics;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.ject.vs.admin.analytics.AnalyticsMetricDef.count;
import static com.ject.vs.admin.analytics.AnalyticsMetricDef.derived;

/**
 * 지금 수집 중인 행동 로그(analytics_events) 전체의 정적 카탈로그.
 *
 * <p>새 이벤트를 추가로 수집하기 시작하면 여기 한 줄만 추가하면 어드민 분석 화면에도 바로 나타난다.
 * 반대로 여기 없는 이벤트는 DB에는 쌓여도 화면에서 고를 수 없다 — 의도적이다. 카탈로그에
 * 이름과 설명을 붙여야만 "무엇을 재는 값인지" 관리자가 알아볼 수 있기 때문이다.
 */
public final class AnalyticsCatalog {

    private static final AnalyticsBreakdown RANK = new AnalyticsBreakdown("rank", "순위");
    private static final AnalyticsBreakdown VARIANT = new AnalyticsBreakdown("variant", "시안(A/B)");
    private static final AnalyticsBreakdown ACTION = new AnalyticsBreakdown("action", "행동");
    private static final AnalyticsBreakdown VOTE_TYPE = new AnalyticsBreakdown("vote_type", "투표 종류(일반/몰입형)");
    private static final AnalyticsBreakdown VOTE_STATUS = new AnalyticsBreakdown("vote_status", "투표 상태(진행중/종료)");
    private static final AnalyticsBreakdown EMOJI_TYPE = new AnalyticsBreakdown("emoji_type", "이모지 종류");
    private static final AnalyticsBreakdown CHAT_EMOJI = new AnalyticsBreakdown("emoji", "채팅 이모지");
    private static final AnalyticsBreakdown CHAT_LIST_STATUS = new AnalyticsBreakdown("status", "채팅 목록 상태(진행중/종료)");
    private static final AnalyticsBreakdown IS_FIRST_MESSAGE = new AnalyticsBreakdown("is_first_message", "첫 메시지 여부");
    private static final AnalyticsBreakdown NOTIFICATION_TYPE = new AnalyticsBreakdown("notification_type", "알림 유형");
    private static final AnalyticsBreakdown PLATFORM = new AnalyticsBreakdown("platform", "플랫폼(iOS/Android)");
    private static final AnalyticsBreakdown SIGNUP_METHOD = new AnalyticsBreakdown("method", "가입 경로");
    private static final AnalyticsBreakdown UTM_SOURCE = new AnalyticsBreakdown("utm_source", "유입 소스(utm_source)");
    private static final AnalyticsBreakdown UTM_MEDIUM = new AnalyticsBreakdown("utm_medium", "유입 매체(utm_medium)");
    private static final AnalyticsBreakdown UTM_CAMPAIGN = new AnalyticsBreakdown("utm_campaign", "유입 캠페인(utm_campaign)");

    private static final String HOME = "홈";
    private static final String IMMERSIVE = "몰입형 투표 (A/B 성과지표)";
    private static final String VOTE = "일반 투표";
    private static final String CHAT = "채팅";
    private static final String NOTIFICATION = "알림";
    private static final String ACCOUNT = "계정 · 유입";

    public static final List<AnalyticsMetricDef> ALL = List.of(
            // ── 홈 ─────────────────────────────────────────────
            count(HOME, "hot_topic_carousel_clicked", "핫토픽 캐러셀 클릭",
                    "핫토픽 TOP5 중 상단 캐러셀(1~3위) 카드를 눌러 투표 상세로 이동", RANK),
            count(HOME, "hot_topic_list_clicked", "핫토픽 리스트 클릭",
                    "핫토픽 TOP5 중 하단 리스트(4~5위) 카드를 눌러 투표 상세로 이동", RANK),
            count(HOME, "all_votes_clicked", "'모든 투표' 카드 클릭",
                    "핫토픽 영역이 아닌 '모든 투표' 리스트에서 카드를 눌러 투표 상세로 이동"),

            // ── 몰입형 투표 (A/B) ──────────────────────────────
            count(IMMERSIVE, "immersive_feed_viewed", "몰입형 피드 로드",
                    "스와이프 피드를 한 번 불러옴(약 10개 콘텐츠 묶음 단위)", VARIANT),
            count(IMMERSIVE, "immersive_content_viewed", "몰입형 콘텐츠 노출",
                    "몰입형 콘텐츠 1건이 화면에 실제로 노출됨 — ① 투표 전환율의 분모", VARIANT),
            count(IMMERSIVE, "immersive_first_action", "몰입형 첫 행동",
                    "노출 1건에서 사용자가 가장 먼저 한 행동 — ③ 첫 행동 분포"
                            + "(action: VOTE/CHAT/EMOJI/SHARE/EXPAND/SCROLL_NEXT)", VARIANT, ACTION),
            count(IMMERSIVE, "immersive_vote_participated", "몰입형 투표 참여/취소",
                    "몰입형 투표 참여 또는 취소 — ① 전환율의 분자(action=VOTED)", VARIANT, ACTION),
            derived("immersive_time_to_vote", IMMERSIVE, "② Time to Vote (평균)",
                    "콘텐츠 노출부터 실제 투표까지 걸린 평균 시간(초). action=VOTED 건만, 시안별로 자동 분해",
                    MetricKind.TIME_TO_VOTE_AVG),
            derived("immersive_bounce", IMMERSIVE, "이탈 (노출만 되고 행동 없음)",
                    "immersive_content_viewed는 있는데 같은 콘텐츠의 immersive_first_action이 없는 건. 시안별로 자동 분해",
                    MetricKind.BOUNCE),
            derived("immersive_first_action_distribution", IMMERSIVE, "몰입형 첫 행동 분포 (시안별)",
                    "조회 기간 전체에서 시안별로 첫 행동(VOTE/CHAT/EMOJI/SHARE/EXPAND/SCROLL_NEXT)을 "
                            + "많은 순으로 나열한다. A안과 B안의 행동 분포를 나란히 비교. 시간 추세 그래프는 없다.",
                    MetricKind.FIRST_ACTION_DISTRIBUTION),
            count(IMMERSIVE, "immersive_live_viewed", "몰입형 실시간 현황 조회",
                    "투표 후 실시간 비율 갱신 폴링(투표함/총 참여자수)", VARIANT),
            count(IMMERSIVE, "share_link_generated", "공유 링크 생성",
                    "투표 공유 링크 생성(일반형·몰입형 공통 이벤트)", VOTE_TYPE, VARIANT),

            // ── 일반 투표 ──────────────────────────────────────
            count(VOTE, "vote_detail_viewed", "일반 투표 상세 조회", "일반형 투표 상세 화면 조회", VOTE_STATUS),
            count(VOTE, "vote_participated", "일반 투표 참여", "일반형 투표에 참여(옵션 선택)"),
            count(VOTE, "vote_canceled", "일반 투표 취소", "'다시 투표하기'로 기존 참여를 취소"),
            count(VOTE, "emoji_reacted", "이모지 반응",
                    "투표에 이모지 반응 추가/변경/취소(action: CREATED/CHANGED/CANCELED)", EMOJI_TYPE, ACTION),
            count(VOTE, "result_page_viewed", "결과 페이지 조회", "투표 결과·AI 인사이트 화면 조회", VOTE_STATUS),
            count(VOTE, "free_votes_checked", "비회원 무료 투표 잔여 확인", "비회원의 남은 무료 투표 횟수 조회"),
            count(VOTE, "free_limit_exceeded", "비회원 무료 투표 소진", "비회원이 무료 투표(5회)를 다 쓰고 다시 시도"),

            // ── 채팅 ───────────────────────────────────────────
            count(CHAT, "chat_list_viewed", "채팅 목록 조회", "참여 중인 투표 채팅방 목록 조회", CHAT_LIST_STATUS),
            count(CHAT, "chat_room_entered", "채팅방 입장", "특정 투표 채팅방 입장", VOTE_STATUS),
            count(CHAT, "chat_messages_viewed", "채팅 메시지 목록 조회", "채팅방 메시지 목록(페이지) 조회"),
            count(CHAT, "chat_message_sent", "채팅 메시지 전송", "채팅방에 메시지 전송", IS_FIRST_MESSAGE),
            count(CHAT, "chat_message_reacted", "채팅 메시지 반응", "채팅 메시지에 이모지 반응", CHAT_EMOJI),
            count(CHAT, "chat_read_updated", "채팅 읽음 처리", "채팅방 읽음 위치 갱신"),

            // ── 알림 ───────────────────────────────────────────
            count(NOTIFICATION, "notification_list_viewed", "알림 목록 조회", "알림 목록 화면 조회"),
            count(NOTIFICATION, "notification_opened", "알림 클릭", "알림을 눌러 상세로 이동", NOTIFICATION_TYPE),
            count(NOTIFICATION, "push_token_registered", "푸시 토큰 등록", "기기 푸시 토큰 등록/갱신", PLATFORM),

            // ── 계정 · 유입 ────────────────────────────────────
            count(ACCOUNT, "signup_completed", "회원가입 완료", "소셜 로그인으로 신규 가입 완료",
                    SIGNUP_METHOD, UTM_SOURCE, UTM_MEDIUM, UTM_CAMPAIGN),
            count(ACCOUNT, "landing_visited", "랜딩 방문", "UTM 파라미터가 실린 랜딩 페이지 최초 방문",
                    UTM_SOURCE, UTM_MEDIUM, UTM_CAMPAIGN)
    );

    private AnalyticsCatalog() {
    }

    public static Optional<AnalyticsMetricDef> find(String id) {
        return ALL.stream().filter(m -> m.id().equals(id)).findFirst();
    }

    /** 주어진 지표 id들 중 하나라도 속한 그룹 이름들. 사이드바에서 선택된 지표가 있는 그룹만 펼쳐두는 데 쓴다. */
    public static Set<String> groupsContainingAny(List<String> metricIds) {
        Set<String> ids = Set.copyOf(metricIds);
        Set<String> groups = new java.util.LinkedHashSet<>();
        for (AnalyticsMetricDef def : ALL) {
            if (ids.contains(def.id())) {
                groups.add(def.groupLabel());
            }
        }
        return groups;
    }

    /** 화면 체크박스를 그룹별로 묶어 렌더링하기 위한 형태. 카탈로그에 등장하는 순서를 그대로 유지한다. */
    public static Map<String, List<AnalyticsMetricDef>> byGroup() {
        Map<String, List<AnalyticsMetricDef>> grouped = new LinkedHashMap<>();
        for (AnalyticsMetricDef def : ALL) {
            grouped.computeIfAbsent(def.groupLabel(), g -> new java.util.ArrayList<>()).add(def);
        }
        return grouped;
    }

    /** "쪼개서 보기" 드롭다운 옵션. 카탈로그 전체에 등장하는 속성 키를 처음 나온 순서대로 중복 없이 모은다. */
    public static List<AnalyticsBreakdown> allBreakdowns() {
        Map<String, AnalyticsBreakdown> byKey = new LinkedHashMap<>();
        byKey.put(AnalyticsBreakdown.NONE.propertyKey(), AnalyticsBreakdown.NONE);
        for (AnalyticsMetricDef def : ALL) {
            for (AnalyticsBreakdown b : def.breakdowns()) {
                byKey.putIfAbsent(b.propertyKey(), b);
            }
        }
        return List.copyOf(byKey.values());
    }

    /**
     * 쪼개기 속성 키 → 그 속성을 실제로 갖는 지표 id들(공백 구분 문자열). 화면의 "쪼개서 보기" 드롭다운을
     * 체크한 지표에 맞게 좁히는 자바스크립트가 각 {@code <option>}의 {@code data-metrics} 속성으로 그대로 쓴다.
     *
     * <p>드롭다운 하나가 모든 지표에 공통으로 걸려 있다 보니, "시안(A/B)"처럼 몰입형 지표에만 있는 속성을
     * 골라도 채팅·알림 같은 다른 지표엔 아무 효과가 없어 헷갈린다는 피드백으로 추가했다. 체크한 지표에
     * 해당 속성이 있는 경우에만 그 옵션을 고를 수 있게 하면, 애초에 안 맞는 조합을 고를 수가 없다.
     */
    public static Map<String, String> breakdownApplicability() {
        Map<String, LinkedHashSet<String>> byKey = new LinkedHashMap<>();
        for (AnalyticsMetricDef def : ALL) {
            for (AnalyticsBreakdown b : def.breakdowns()) {
                byKey.computeIfAbsent(b.propertyKey(), k -> new LinkedHashSet<>()).add(def.id());
            }
        }
        Map<String, String> joined = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> e : byKey.entrySet()) {
            joined.put(e.getKey(), String.join(" ", e.getValue()));
        }
        return joined;
    }
}
