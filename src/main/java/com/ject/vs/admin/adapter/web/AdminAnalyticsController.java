package com.ject.vs.admin.adapter.web;

import com.ject.vs.admin.analytics.AdminAnalyticsService;
import com.ject.vs.admin.port.AdminAuthorizer;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 어드민 이벤트 분석 화면(Thymeleaf). 기간(일시 단위) + 지표(다중 선택) + 쪼개서 보기 조건을
 * GET 쿼리 파라미터로 받아 화면을 그린다 — 상태가 URL에 그대로 실려 새로고침·북마크·공유가 된다.
 */
@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private static final String VIEW = "admin/analytics";
    private static final String VIEW_DENIED = "admin/denied";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_RANGE_DAYS = 6;

    private final AdminAnalyticsService adminAnalyticsService;
    private final AdminAuthorizer adminAuthorizer;
    private final Clock clock;

    @GetMapping
    public String page(@AuthenticationPrincipal Long userId,
                       @RequestParam(required = false) String from,
                       @RequestParam(required = false) String to,
                       @RequestParam(required = false) List<String> metrics,
                       @RequestParam(defaultValue = "") String breakdown,
                       Model model, HttpServletResponse response) {
        if (!adminAuthorizer.isAdmin(userId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            model.addAttribute("userId", userId);
            return VIEW_DENIED;
        }

        LocalDate today = LocalDate.now(clock.withZone(KST));
        LocalDateTime[] range = adminAnalyticsService.clampRange(
                parseOrDefault(from, today.minusDays(DEFAULT_RANGE_DAYS).atStartOfDay()),
                parseOrDefault(to, today.atTime(23, 59)));
        String selectedBreakdown = adminAnalyticsService.normalizeBreakdown(breakdown);
        List<String> selectedMetrics = metrics == null ? List.of() : metrics;

        model.addAttribute("groups", adminAnalyticsService.catalogGroups());
        model.addAttribute("breakdownOptions", adminAnalyticsService.breakdownOptions());
        model.addAttribute("breakdownApplicability", adminAnalyticsService.breakdownApplicability());
        model.addAttribute("expandedGroups", adminAnalyticsService.groupsContainingAny(selectedMetrics));
        model.addAttribute("from", range[0]);
        model.addAttribute("to", range[1]);
        model.addAttribute("selectedMetrics", selectedMetrics);
        model.addAttribute("selectedBreakdown", selectedBreakdown);

        if (!selectedMetrics.isEmpty()) {
            model.addAttribute("result",
                    adminAnalyticsService.query(selectedMetrics, range[0], range[1], selectedBreakdown));
        }

        return VIEW;
    }

    /** {@code datetime-local} 입력(예: 2026-09-02T14:30)을 파싱한다. 초는 항상 생략되어 온다. */
    private LocalDateTime parseOrDefault(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
