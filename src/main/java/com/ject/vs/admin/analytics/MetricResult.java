package com.ject.vs.admin.analytics;

import java.util.List;

/**
 * 지표 하나를 조회한 결과. 화면(Thymeleaf)이 그대로 뿌릴 수 있도록 표시 문자열까지 이 안에서 만든다.
 *
 * @param def         조회한 지표 정의(제목·설명 표시용).
 * @param summary     카드 상단에 보여줄 한 줄 총계. 예: "총 1,234건", "평균 6.4초 (표본 89건)".
 * @param tableHeaders 표 헤더. 첫 칸은 "날짜", 이후는 시리즈 이름.
 * @param tableRows   표 본문. 각 행은 [날짜문자열, 시리즈1 값, 시리즈2 값, ...] 순서로 tableHeaders와 맞는다.
 * @param chartSvg    완성된 {@code <svg>...</svg>} 문자열.
 * @param legendHtml  차트 아래 범례 HTML. 시리즈가 하나뿐이면 빈 문자열.
 */
public record MetricResult(
        AnalyticsMetricDef def,
        String summary,
        List<String> tableHeaders,
        List<List<String>> tableRows,
        String chartSvg,
        String legendHtml
) {
}
