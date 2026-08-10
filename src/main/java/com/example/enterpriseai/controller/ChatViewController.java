/*
 * =============================================================================
 * 클래스명 : ChatViewController
 * =============================================================================
 * 목적
 *  - 사용자를 채팅 화면으로 이동시킨다.
 *  - 현재 단계에서는 채팅 화면만 제공한다.
 *  - 이후 Spring Security 적용 후 로그인 사용자만 접근하도록 변경한다.
 */

package com.example.enterpriseai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatViewController {

    // 채팅 화면을 반환한다.
    @GetMapping("/")
    public String home() {

        return "chat";
    }

    // 채팅 화면을 반환한다.
    @GetMapping("/chat")
    public String chat() {

        return "chat";
    }

}