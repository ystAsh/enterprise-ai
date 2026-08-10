/*
 * =============================================================================
 * 클래스명 : Role
 * =============================================================================
 * 목적
 *  - MSSQL roles 테이블을 JPA에서 표현한다.
 *  - 로그인 사용자의 Spring Security Role 정보를 조회할 때 사용한다.
 */

package com.example.enterpriseai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    // Spring Security에서 사용할 역할 코드이다.
    @Column(
            name = "role_code",
            nullable = false,
            length = 50
    )
    private String roleCode;

    // 화면이나 관리 기능에서 사용할 역할명이다.
    @Column(
            name = "role_name",
            nullable = false,
            length = 100
    )
    private String roleName;

    // 비활성화된 Role은 로그인 권한으로 사용하지 않는다.
    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled;

    protected Role() {
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}