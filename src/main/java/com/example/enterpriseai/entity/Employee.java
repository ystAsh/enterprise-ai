/*
 * =============================================================================
 * 클래스명 : Employee
 * =============================================================================
 * 목적
 *  - MSSQL employees 테이블의 직원 업무 데이터를 표현한다.
 *  - Database RAG에서 로그인 사용자의 조직/부서 범위에 맞는
 *    직원 수 및 직원 정보를 조회하기 위한 기준 Entity로 사용한다.
 */

package com.example.enterpriseai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    // 직원이 소속된 조직이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "organization_id",
            nullable = false
    )
    private Organization organization;

    // 직원이 소속된 부서이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "department_id",
            nullable = false
    )
    private Department department;

    @Column(
            name = "employee_name",
            nullable = false,
            length = 100
    )
    private String employeeName;

    @Column(
            name = "employee_no",
            nullable = false,
            unique = true,
            length = 30
    )
    private String employeeNo;

    @Column(
            name = "position_name",
            length = 100
    )
    private String positionName;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    protected Employee() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Department getDepartment() {
        return department;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public String getPositionName() {
        return positionName;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}