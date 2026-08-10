/*
 * =============================================================================
 * 클래스명 : LoginController
 * =============================================================================
 * 목적
 *  - 사용자에게 로그인 화면을 제공한다.
 *  - Spring Security의 formLogin 인증 흐름과 login.html을 연결한다.
 */

package com.example.enterpriseai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // 로그인 화면을 반환한다.
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}