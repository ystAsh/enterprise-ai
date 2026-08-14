/*
 * =============================================================================
 * 클래스명 : CsrfController
 * =============================================================================
 * 목적
 *  - React 클라이언트에 Spring Security CSRF 토큰을 제공한다.
 *  - 세션 기반 POST 요청의 CSRF 공격을 방지하기 위해 사용한다.
 */

package com.example.enterpriseai.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// JSON을 반환하는 REST Controller로 등록한다.
@RestController

// 이 Controller의 기본 API 경로를 설정한다.
@RequestMapping("/api/auth")
public class CsrfController {

    // React가 로그인 전에 사용할 CSRF 토큰을 반환한다.
    @GetMapping("/csrf")
    public CsrfToken csrf(
            CsrfToken csrfToken
    ) {
        return csrfToken;
    }
}