package com.ject.vs.admin.analytics;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param rangeLabel 화면 상단에 보여줄 조회 기간 한 줄. 예: "2026-08-27 00:00 ~ 2026-09-02 23:59 (KST, 일 단위)".
 */
public record AnalyticsQueryResult(LocalDateTime from, LocalDateTime to, String rangeLabel, List<MetricResult> metrics) {
}
