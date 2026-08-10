/*
 * =============================================================================
 * 클래스명 : Department
 * =============================================================================
 * 목적
 *  - MSSQL departments 테이블을 JPA에서 표현한다.
 *  - 로그인 사용자의 부서 정보를 조회할 때 사용한다.
 */

package com.example.enterpriseai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    // 부서가 소속된 조직이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "organization_id",
            nullable = false
    )
    private Organization organization;

    @Column(
            name = "department_code",
            nullable = false,
            length = 50
    )
    private String departmentCode;

    @Column(
            name = "department_name",
            nullable = false,
            length = 100
    )
    private String departmentName;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled;

    protected Department() {
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}