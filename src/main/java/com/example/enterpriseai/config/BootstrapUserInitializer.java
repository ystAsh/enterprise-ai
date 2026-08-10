/*
 * =============================================================================
 * 클래스명 : BootstrapUserInitializer
 * =============================================================================
 * 목적
 *  - 개발환경에서 최초 로그인 테스트 사용자를 생성한다.
 *  - 아이디와 비밀번호는 소스코드가 아니라 환경변수에서 전달받는다.
 *  - 실제 비밀번호 저장은 UserRegistrationService의 BCrypt 처리를 사용한다.
 */

package com.example.enterpriseai.config;

import com.example.enterpriseai.service.security.UserRegistrationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// Spring Boot가 시작된 후 최초 사용자 생성 작업을 실행한다.
@Component
public class BootstrapUserInitializer
        implements ApplicationRunner {

    private final UserRegistrationService userRegistrationService;
    private final Environment environment;

    public BootstrapUserInitializer(
            UserRegistrationService userRegistrationService,
            Environment environment
    ) {
        this.userRegistrationService = userRegistrationService;
        this.environment = environment;
    }

    // 환경변수가 설정된 경우에만 최초 로그인 사용자를 생성한다.
    @Override
    public void run(
            ApplicationArguments args
    ) {

        String username =
                environment.getProperty(
                        "BOOTSTRAP_USER_USERNAME"
                );

        String password =
                environment.getProperty(
                        "BOOTSTRAP_USER_PASSWORD"
                );

        // 환경변수가 없으면 사용자 생성 자체를 수행하지 않는다.
        if (username == null
                || username.isBlank()
                || password == null
                || password.isBlank()) {
            return;
        }

        // 비밀번호는 UserRegistrationService에서 BCrypt로 변환한 뒤 저장한다.
        userRegistrationService.createTestUser(
                username,
                password
        );
    }
}