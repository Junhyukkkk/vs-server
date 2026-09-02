package com.ject.vs.admin.analytics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 지표를 인라인 SVG 선 그래프로 그린다.
 *
 * <p>이 프로젝트의 다른 어드민 페이지들과 마찬가지로 외부 차트 라이브러리를 쓰지 않는다.
 * 값이 전부 이 클래스 안에서 만든 숫자/날짜뿐이라 문자열 조립이 안전하다(사용자 입력이
 * 그대로 SVG에 꽂히지 않는다 — 시리즈 이름만 예외이며 {@link #escape}로 이스케이프한다).
 *
 * <p>선 색상 외의 나머지(축, 격자, 글자색)는 CSS 커스텀 프로퍼티({@code var(--muted)} 등)를
 * 그대로 참조한다. 인라인 SVG는 페이지의 CSS를 상속하므로 라이트/다크 모두 자동으로 맞는다.
 */
final class SvgChartRenderer {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 220;
    private static final int PAD_LEFT = 44;
    private static final int PAD_RIGHT = 16;
    private static final int PAD_TOP = 16;
    private static final int PAD_BOTTOM = 28;
    private static final int MAX_X_LABELS = 7;

    /** 시리즈 순서대로 순환하는 팔레트. 라이트/다크 배경 둘 다에서 어느 정도 대비가 나오는 색으로 골랐다. */
    private static final String[] PALETTE = {
            "#3b5bfd", "#e07a1f", "#1f9d6b", "#d1425c", "#8a5bd6", "#1fa0ad", "#c98a1f"
    };

    private SvgChartRenderer() {
    }

    /**
     * @param buckets      x축에 올릴 시간 구간(오름차순, 빈 구간 없이 연속). 조회 기간에 따라 일 단위·시간 단위 둘 다 올 수 있다.
     * @param seriesLabels 그릴 시리즈 이름(범례 겸 팔레트 인덱스 순서).
     * @param values       시리즈명 → (구간 → 값). 값이 없는 (시리즈, 구간)은 0으로 취급.
     * @param axisFormat   x축 라벨 형식. 일 단위면 "MM/dd", 시간 단위면 "MM/dd HH:mm" 등 호출부가 정한다.
     */
    static String render(List<LocalDateTime> buckets, List<String> seriesLabels,
                          Map<String, Map<LocalDateTime, Double>> values, DateTimeFormatter axisFormat) {
        if (buckets.isEmpty() || seriesLabels.isEmpty()) {
            return "<p class=\"empty\">표시할 데이터가 없습니다.</p>";
        }

        double max = seriesLabels.stream()
                .flatMap(s -> values.getOrDefault(s, Map.of()).values().stream())
                .mapToDouble(Double::doubleValue)
                .max().orElse(0);
        double yMax = max <= 0 ? 1 : max * 1.15;

        int plotWidth = WIDTH - PAD_LEFT - PAD_RIGHT;
        int plotHeight = HEIGHT - PAD_TOP - PAD_BOTTOM;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox=\"0 0 ").append(WIDTH).append(' ').append(HEIGHT)
                .append("\" class=\"chart-svg\" role=\"img\" aria-label=\"기간별 추이 그래프\">");

        // 기준선(0, 최대)
        svg.append(line(PAD_LEFT, HEIGHT - PAD_BOTTOM, WIDTH - PAD_RIGHT, HEIGHT - PAD_BOTTOM, "var(--border)"));
        svg.append(text(PAD_LEFT - 6, HEIGHT - PAD_BOTTOM, "0", "end"));
        svg.append(text(PAD_LEFT - 6, PAD_TOP + 4, formatAxisValue(yMax), "end"));

        // x축 시간 라벨(최대 7개, 균등 간격)
        int bucketCount = buckets.size();
        int labelStep = Math.max(1, (int) Math.ceil(bucketCount / (double) MAX_X_LABELS));
        for (int i = 0; i < bucketCount; i += labelStep) {
            double x = PAD_LEFT + (bucketCount == 1 ? plotWidth / 2.0 : plotWidth * i / (double) (bucketCount - 1));
            svg.append(text(x, HEIGHT - PAD_BOTTOM + 16, axisFormat.format(buckets.get(i)), "middle"));
        }

        // 시리즈별 선
        for (int s = 0; s < seriesLabels.size(); s++) {
            String label = seriesLabels.get(s);
            String color = PALETTE[s % PALETTE.length];
            Map<LocalDateTime, Double> series = values.getOrDefault(label, Map.of());

            StringBuilder points = new StringBuilder();
            for (int i = 0; i < bucketCount; i++) {
                double x = PAD_LEFT + (bucketCount == 1 ? plotWidth / 2.0 : plotWidth * i / (double) (bucketCount - 1));
                double v = series.getOrDefault(buckets.get(i), 0.0);
                double y = HEIGHT - PAD_BOTTOM - (v / yMax) * plotHeight;
                if (i > 0) points.append(' ');
                points.append(round1(x)).append(',').append(round1(y));
            }
            svg.append("<polyline points=\"").append(points)
                    .append("\" fill=\"none\" stroke=\"").append(color)
                    .append("\" stroke-width=\"2\" stroke-linejoin=\"round\" stroke-linecap=\"round\"/>");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /** 범례. 차트 아래 일반 HTML로 그린다 — SVG 안에 텍스트를 늘어놓는 것보다 줄바꿈 대응이 쉽다. */
    static String legend(List<String> seriesLabels) {
        if (seriesLabels.size() <= 1) {
            return "";
        }
        StringBuilder html = new StringBuilder("<div class=\"legend\">");
        for (int i = 0; i < seriesLabels.size(); i++) {
            String color = PALETTE[i % PALETTE.length];
            html.append("<span class=\"legend-item\"><i style=\"background:").append(color).append("\"></i>")
                    .append(escape(seriesLabels.get(i))).append("</span>");
        }
        return html.append("</div>").toString();
    }

    private static String line(double x1, double y1, double x2, double y2, String color) {
        return "<line x1=\"%s\" y1=\"%s\" x2=\"%s\" y2=\"%s\" stroke=\"%s\" stroke-width=\"1\"/>"
                .formatted(round1(x1), round1(y1), round1(x2), round1(y2), color);
    }

    private static String text(double x, double y, String content, String anchor) {
        return "<text x=\"%s\" y=\"%s\" text-anchor=\"%s\" class=\"chart-label\">%s</text>"
                .formatted(round1(x), round1(y), anchor, escape(content));
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static String formatAxisValue(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : "%.1f".formatted(v);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
