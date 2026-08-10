/*
 * =============================================================================
 * 클래스명 : OrganizationRepository
 * =============================================================================
 * 목적
 *  - MSSQL organizations 테이블의 조직 정보를 조회한다.
 *  - 사용자 생성 시 organizationCode 기준으로 조직을 찾는다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Organization Entity의 기본 CRUD 기능을 제공하는 JPA Repository이다.
// 기본키 타입은 Long을 사용한다.
public interface OrganizationRepository
        extends JpaRepository<Organization, Long> {

    // 조직 코드로 활성 조직 정보를 조회할 때 사용한다.
    Optional<Organization> findByOrganizationCodeAndEnabledTrue(
            String organizationCode
    );
}