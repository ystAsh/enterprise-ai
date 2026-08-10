/*
 * =============================================================================
 * 클래스명 : DepartmentRepository
 * =============================================================================
 * 목적
 *  - MSSQL departments 테이블의 부서 정보를 조회한다.
 *  - 사용자 생성 시 특정 조직에 소속된 활성 부서를 조회한다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Department Entity의 기본 CRUD 기능을 제공하는 JPA Repository이다.
// 기본키 타입은 Long을 사용한다.
public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    // 조직 ID와 부서 코드가 모두 일치하는 활성 부서만 조회한다.
    // 같은 부서 코드가 다른 조직에 존재할 가능성을 고려해 조직 조건을 함께 강제한다.
    Optional<Department> findByOrganizationOrganizationIdAndDepartmentCodeAndEnabledTrue(
            Long organizationId,
            String departmentCode
    );
}