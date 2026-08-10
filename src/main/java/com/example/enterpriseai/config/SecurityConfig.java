/*
 * =============================================================================
 * 클래스명 : SecurityConfig
 * =============================================================================
 * 목적
 *  - Spring Security 기반 로그인/로그아웃/접근 제어 정책을 설정한다.
 *  - 비로그인 사용자의 채팅 화면 접근을 차단한다.
 *  - BCrypt 비밀번호 검증과 세션 기반 인증을 사용한다.
 */

package com.example.enterpriseai.config;

import com.example.enterpriseai.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService
    ) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호는 평문 비교하지 않고 BCrypt 해시로 검증한다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .userDetailsService(customUserDetailsService)

                .authorizeHttpRequests(auth -> auth
                        // 로그인 화면과 정적 리소스만 비로그인 접근을 허용한다.
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // 그 외 모든 요청은 로그인된 사용자만 접근할 수 있다.
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        // 직접 만든 로그인 화면을 사용한다.
                        .loginPage("/login")
                        .defaultSuccessUrl("/chat", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        // 로그아웃 시 세션을 무효화하고 인증 쿠키를 제거한다.
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                .sessionManagement(session -> session
                        // 로그인 성공 시 세션 ID를 변경하여 세션 고정 공격을 방지한다.
                        .sessionFixation(fixation ->
                                fixation.migrateSession()
                        )
                );

        return http.build();
    }
}