package com.ject.vs.admin.analytics;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsQueryResult(LocalDate from, LocalDate to, List<MetricResult> metrics) {
}
