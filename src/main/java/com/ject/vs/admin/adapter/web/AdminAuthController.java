package com.ject.vs.admin.adapter.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 어드민 페이지 진입 안내. 실제 로그인은 서비스와 동일한 구글 OAuth2를 그대로 사용한다.
 */
@Controller
public class AdminAuthController {

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }
}
