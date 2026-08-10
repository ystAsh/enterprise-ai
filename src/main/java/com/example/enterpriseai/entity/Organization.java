/*
 * =============================================================================
 * 클래스명 : Organization
 * =============================================================================
 * 목적
 *  - MSSQL organizations 테이블을 JPA에서 표현한다.
 *  - 로그인 사용자의 조직 정보를 조회할 때 사용한다.
 */

package com.example.enterpriseai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(
            name = "organization_code",
            nullable = false,
            length = 50
    )
    private String organizationCode;

    @Column(
            name = "organization_name",
            nullable = false,
            length = 100
    )
    private String organizationName;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled;

    protected Organization() {
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}