package com.ject.vs.admin.analytics;

import java.util.List;

/**
 * 지표 하나를 조회한 결과. 화면(Thymeleaf)이 그대로 뿌릴 수 있도록 표시 문자열까지 이 안에서 만든다.
 *
 * @param def              조회한 지표 정의(제목·설명 표시용).
 * @param breakdownCaption 이 카드가 실제로 무엇을 기준으로 쪼개졌는지 한 줄로 되짚어주는 문장.
 *                         예: "쪼개기 기준: 시안(A/B)" / "쪼개기 기준: 전체(안 쪼갬)" /
 *                         "이 지표엔 그 쪼개기 기준이 없어요 → 전체로 표시". 화면 위쪽의 전역
 *                         드롭다운 선택이 카드마다 실제로 적용됐는지 아닌지를 명시해, "위에서 고른 것과
 *                         카드 안 색깔 범례가 무슨 관계인지 모르겠다"는 혼란을 없앤다.
 * @param summary          카드 상단에 보여줄 한 줄 총계. 예: "총 1,234건", "평균 6.4초 (표본 89건)".
 * @param tableHeaders     표 헤더. 첫 칸은 "날짜" 또는 "시각"(조회 단위에 따라 다름), 이후는 시리즈 이름.
 * @param tableRows        표 본문. 각 행은 [시간칸, 시리즈1 값, 시리즈2 값, ...] 순서로 tableHeaders와 맞는다.
 * @param chartSvg         완성된 {@code <svg>...</svg>} 문자열.
 * @param legendHtml       차트 아래 범례 HTML. 시리즈가 하나뿐이면 빈 문자열.
 */
public record MetricResult(
        AnalyticsMetricDef def,
        String breakdownCaption,
        String summary,
        List<String> tableHeaders,
        List<List<String>> tableRows,
        String chartSvg,
        String legendHtml
) {
}
