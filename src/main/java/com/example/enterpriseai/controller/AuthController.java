/*
 * =============================================================================
 * 클래스명 : AuthController
 * =============================================================================
 * 목적
 *  - React 로그인 요청을 받아 Spring Security 인증을 수행한다.
 *  - 인증 성공 시 Spring Security 세션에 인증정보를 저장한다.
 *  - 로그인 성공 상태를 MSSQL에 기록한다.
 */

package com.example.enterpriseai.controller;

import com.example.enterpriseai.entity.AppUser;
import com.example.enterpriseai.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.enterpriseai.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
// JSON 기반 인증 API를 제공하는 REST Controller로 등록한다.
@RestController

// 인증 관련 API의 기본 URL을 지정한다.
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;

    // 인증 성공 정보를 HTTP Session에 저장할 때 사용한다.
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            AuthenticationManager authenticationManager,
            AppUserRepository appUserRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
    }

    // React에서 전달한 아이디/비밀번호로 Spring Security 인증을 수행한다.
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<Void> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        // username/password만 인증에 사용하며 권한정보는 클라이언트에서 받지 않는다.
        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.username(),
                        loginRequest.password()
                );

        // CustomUserDetailsService + BCrypt를 이용해 실제 인증을 수행한다.
        Authentication authentication =
                authenticationManager.authenticate(
                        authenticationRequest
                );

        // 인증 완료 정보를 새로운 SecurityContext에 저장한다.
        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);

        // 인증정보를 HTTP Session에 저장하여 이후 요청에서도 로그인 상태를 유지한다.
        securityContextRepository.saveContext(
                securityContext,
                request,
                response
        );

        // 로그인 성공 시 실패 횟수를 초기화하고 마지막 로그인 시간을 기록한다.
        AppUser appUser = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "인증된 사용자 정보를 찾을 수 없습니다."
                        )
                );

        appUser.recordLoginSuccess();

        return ResponseEntity.ok().build();
    }

    // 로그인 API에서 받을 최소한의 입력값만 정의한다.
    public record LoginRequest(
            String username,
            String password
    ) {
    }

    // 현재 Spring Security 세션에 로그인된 사용자 정보를 반환한다.
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        // React에는 화면에 필요한 최소 사용자 정보만 반환한다.
        List<String> roles =
                currentUser.getAuthorities()
                        .stream()
                        .map(authority ->
                                authority.getAuthority()
                        )
                        .toList();

        return ResponseEntity.ok(
                new CurrentUserResponse(
                        currentUser.getUsername(),
                        roles
                )
        );
    }

    // React 화면에 전달할 최소 사용자 정보이다.
// 내부 userId, organizationId, departmentId, securityLevel은 노출하지 않는다.
    public record CurrentUserResponse(
            String username,
            List<String> roles
    ) {
    }
}