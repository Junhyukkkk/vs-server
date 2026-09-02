package com.ject.vs.admin.analytics;

import java.util.List;

/**
 * 어드민 분석 화면에서 고를 수 있는 지표 하나.
 *
 * <p>대부분은 실제 이벤트 이름과 1:1({@code kind=COUNT})이지만, "Time to Vote 평균"과
 * "이탈"처럼 이벤트 하나를 세는 것만으로는 안 나오는 파생 지표도 같은 목록에 섞여 있다.
 * 화면과 서비스는 {@code id}만으로 지표를 가리키고, 실제 계산은 {@code kind}로 분기한다.
 *
 * @param id          선택 값(체크박스 value). COUNT는 이벤트 이름 그대로, 파생 지표는 별도 id.
 * @param groupLabel  카탈로그 화면에서 묶어 보여줄 그룹(홈/몰입형 투표/일반 투표/채팅/알림/계정).
 * @param title       화면에 보여줄 이름.
 * @param description 이 지표가 무엇을 세는지 한 줄 설명.
 * @param kind        계산 방식.
 * @param eventName   {@code kind=COUNT}일 때 조회할 실제 이벤트 이름. 파생 지표는 null.
 * @param breakdowns  이 지표를 쪼개 볼 수 있는 속성 목록. COUNT에서만 의미가 있고, 없으면 빈 리스트.
 */
public record AnalyticsMetricDef(
        String id,
        String groupLabel,
        String title,
        String description,
        MetricKind kind,
        String eventName,
        List<AnalyticsBreakdown> breakdowns
) {

    static AnalyticsMetricDef count(String groupLabel, String eventName, String title, String description,
                                     AnalyticsBreakdown... breakdowns) {
        return new AnalyticsMetricDef(eventName, groupLabel, title, description,
                MetricKind.COUNT, eventName, List.of(breakdowns));
    }

    static AnalyticsMetricDef derived(String id, String groupLabel, String title, String description, MetricKind kind) {
        return new AnalyticsMetricDef(id, groupLabel, title, description, kind, null, List.of());
    }
}
