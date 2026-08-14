/*
 * =============================================================================
 * 클래스명 : SecurityConfig
 * =============================================================================
 * 목적
 *  - React REST API 기반 Spring Security 인증/인가 정책을 설정한다.
 *  - BCrypt 비밀번호 검증과 서버 세션 기반 인증을 사용한다.
 *  - CSRF 보호를 유지하여 세션 기반 인증 요청을 보호한다.
 *  - 인증되지 않은 REST 요청은 로그인 화면이 아니라 HTTP 401을 반환한다.
 */

package com.example.enterpriseai.config;

import com.example.enterpriseai.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

// Spring Security 설정 클래스로 등록한다.
@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService
    ) {
        this.customUserDetailsService = customUserDetailsService;
    }

    // 비밀번호는 평문 비교하지 않고 BCrypt 해시로 검증한다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // REST 로그인 API에서 username/password 인증을 실행할 때 사용한다.
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // HTTP 요청별 인증, 세션, CSRF, 로그아웃 정책을 설정한다.
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // MSSQL에서 사용자를 조회하는 UserDetailsService를 사용한다.
                .userDetailsService(customUserDetailsService)

                // React + 세션 인증에서도 CSRF 보호를 유지한다.
                // React가 CSRF 토큰을 읽을 수 있도록 XSRF-TOKEN 쿠키를 사용한다.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse()
                        )
                )

                // API별 접근 권한을 설정한다.
                .authorizeHttpRequests(auth -> auth
                        // 로그인과 CSRF 토큰 발급 API만 비로그인 접근을 허용한다.
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/csrf"
                        ).permitAll()

                        // 나머지 모든 API는 로그인된 사용자만 접근할 수 있다.
                        .anyRequest().authenticated()
                )

                // React가 로그인 화면을 담당하므로 Spring formLogin은 사용하지 않는다.
                .formLogin(form -> form.disable())

                // HTTP Basic 인증도 사용하지 않는다.
                .httpBasic(basic -> basic.disable())

                // 로그인 성공 시 필요한 경우에만 서버 세션을 생성한다.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )

                        // 로그인 후 기존 세션 ID를 변경하여 세션 고정 공격을 방지한다.
                        .sessionFixation(fixation ->
                                fixation.migrateSession()
                        )
                )

                // React용 REST 로그아웃 URL을 설정한다.
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(
                                new HttpStatusReturningLogoutSuccessHandler(
                                        HttpStatus.OK
                                )
                        )
                )

                // 인증되지 않은 REST 요청은 로그인 페이지로 보내지 않고 401을 반환한다.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                );

        return http.build();
    }
}