/*
 * =============================================================================
 * 클래스명 : AppUserRepository
 * =============================================================================
 * 목적
 *  - MSSQL app_users 테이블의 사용자 정보를 조회한다.
 *  - Spring Security 로그인 시 username 기준으로 사용자를 조회한다.
 *  - 신규 사용자 생성 시 username 중복 여부를 확인한다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// AppUser Entity의 기본 CRUD 기능을 제공하는 JPA Repository이다.
// 기본키 타입은 Long을 사용한다.
public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    // 로그인 시 Role, 조직, 부서까지 한 번에 조회한다.
    @EntityGraph(attributePaths = {
            "roles",
            "organization",
            "department"
    })
    Optional<AppUser> findByUsername(
            String username
    );

    // 신규 사용자 생성 전에 동일 username이 존재하는지 확인한다.
    boolean existsByUsername(
            String username
    );
}