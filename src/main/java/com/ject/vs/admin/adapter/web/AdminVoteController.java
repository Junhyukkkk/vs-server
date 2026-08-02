package com.ject.vs.admin.adapter.web;

import com.ject.vs.admin.adapter.web.dto.AdminVoteForm;
import com.ject.vs.admin.adapter.web.dto.DurationOption;
import com.ject.vs.admin.port.AdminVoteService;
import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.vote.port.in.VoteCommandUseCase.VoteCreateResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 운영자용 투표 생성 어드민 페이지(Thymeleaf).
 *
 * <p>인증은 서비스와 동일한 access_token 쿠키를 그대로 쓰고,
 * 권한은 {@code admin.user-ids}(ADMIN_USER_IDS 환경변수)에 등록된 userId만 허용한다.
 */
@Slf4j
@Controller
@RequestMapping("/admin/votes")
@RequiredArgsConstructor
public class AdminVoteController {

    private static final String VIEW_CREATE = "admin/vote-create";
    private static final String VIEW_DENIED = "admin/denied";

    private static final DateTimeFormatter END_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AdminVoteService adminVoteService;

    @GetMapping
    public String page(@AuthenticationPrincipal Long userId, Model model, HttpServletResponse response) {
        if (!adminVoteService.isAdmin(userId)) {
            return denied(userId, model, response);
        }
        AdminVoteForm form = new AdminVoteForm();
        if (!adminVoteService.isImageUploadAvailable()) {
            form.setImageSource(AdminVoteForm.ImageSource.URL);
        }
        model.addAttribute("form", form);
        populate(model);
        return VIEW_CREATE;
    }

    @PostMapping
    public String create(@AuthenticationPrincipal Long userId,
                         @Valid @ModelAttribute("form") AdminVoteForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes,
                         HttpServletResponse response) {
        if (!adminVoteService.isAdmin(userId)) {
            return denied(userId, model, response);
        }
        if (bindingResult.hasErrors()) {
            populate(model);
            return VIEW_CREATE;
        }

        try {
            VoteCreateResult result = adminVoteService.create(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "투표 #%d 생성 완료. 종료 예정 %s (KST)".formatted(
                            result.voteId(), END_AT_FORMATTER.format(result.endAt().atZone(KST))));
            return "redirect:/admin/votes";
        } catch (RuntimeException e) {
            log.warn("어드민 투표 생성 실패 (userId={})", userId, e);
            model.addAttribute("errorMessage", describe(e));
            populate(model);
            return VIEW_CREATE;
        }
    }

    private void populate(Model model) {
        model.addAttribute("durations", DurationOption.all());
        model.addAttribute("recentVotes", adminVoteService.findRecentVotes());
        model.addAttribute("imageUploadAvailable", adminVoteService.isImageUploadAvailable());
    }

    private String denied(Long userId, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("userId", userId);
        return VIEW_DENIED;
    }

    private String describe(RuntimeException e) {
        if (e instanceof BusinessException businessException) {
            return businessException.getErrorCode().getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : "투표 생성 중 오류가 발생했습니다.";
    }
}
