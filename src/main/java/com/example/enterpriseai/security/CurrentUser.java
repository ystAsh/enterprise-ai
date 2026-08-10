/*
 * =============================================================================
 * 클래스명 : CurrentUser
 * =============================================================================
 * 목적
 *  - Spring Security가 인증한 현재 로그인 사용자 정보를 보관한다.
 *  - userId, organizationId, departmentId, securityLevel, Role을 서버에서 관리한다.
 *  - RAG 및 Database 조회 시 클라이언트 입력값 대신 이 정보를 보안 기준으로 사용한다.
 */

package com.example.enterpriseai.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CurrentUser extends User {

    private final Long userId;
    private final Long organizationId;
    private final Long departmentId;
    private final int securityLevel;

    public CurrentUser(
            Long userId,
            String username,
            String password,
            Long organizationId,
            Long departmentId,
            int securityLevel,
            boolean enabled,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(
                username,
                password,
                enabled,
                true,
                true,
                accountNonLocked,
                authorities
        );

        this.userId = userId;
        this.organizationId = organizationId;
        this.departmentId = departmentId;
        this.securityLevel = securityLevel;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }
}