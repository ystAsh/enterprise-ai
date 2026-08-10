/*
 * =============================================================================
 * 클래스명 : RoleRepository
 * =============================================================================
 * 목적
 *  - MSSQL roles 테이블의 Role 정보를 조회한다.
 *  - 사용자 생성 시 활성화된 Role을 roleCode 기준으로 조회한다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Role Entity의 기본 CRUD 기능을 제공하는 JPA Repository이다.
// 기본키 타입은 Long을 사용한다.
public interface RoleRepository
        extends JpaRepository<Role, Long> {

    // 활성화된 Role만 roleCode 기준으로 조회한다.
    Optional<Role> findByRoleCodeAndEnabledTrue(
            String roleCode
    );
}