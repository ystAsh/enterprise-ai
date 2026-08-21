/*
 * =============================================================================
 * 클래스명 : EmployeeRepository
 * =============================================================================
 * 목적
 *  - MSSQL employees 테이블의 직원 데이터를 조회한다.
 *  - Database RAG에서 검증된 직원 업무 Query를 제공한다.
 *  - 로그인 사용자의 조직과 부서 범위를 Query 조건으로 강제할 수 있도록 한다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    /*
     * 특정 조직과 부서에 소속된
     * 재직 중인 직원 수를 조회한다.
     *
     * organizationId와 departmentId는
     * 브라우저나 LLM에서 전달받지 않고
     * CurrentUser의 값을 Service에서 전달한다.
     */
    long countByOrganization_OrganizationIdAndDepartment_DepartmentIdAndActiveTrue(
            Long organizationId,
            Long departmentId
    );
}