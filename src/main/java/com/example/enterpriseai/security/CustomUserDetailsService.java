/*
 * =============================================================================
 * 클래스명 : CustomUserDetailsService
 * =============================================================================
 * 목적
 *  - MSSQL에서 로그인 사용자를 조회한다.
 *  - 사용자 권한과 조직/부서/보안등급 정보를 CurrentUser로 구성한다.
 *  - Spring Security 세션에서 신뢰할 사용자 보안정보를 제공한다.
 */

package com.example.enterpriseai.security;

import com.example.enterpriseai.entity.AppUser;
import com.example.enterpriseai.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(
            AppUserRepository appUserRepository
    ) {
        this.appUserRepository = appUserRepository;
    }

    // username 기준으로 사용자를 조회하고 CurrentUser로 변환한다.
    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        AppUser appUser = appUserRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Invalid username or password"
                        )
                );

        // 활성화된 Role만 Spring Security 권한으로 변환한다.
        List<SimpleGrantedAuthority> authorities =
                appUser.getRoles()
                        .stream()
                        .filter(role -> role.isEnabled())
                        .map(role ->
                                new SimpleGrantedAuthority(
                                        role.getRoleCode()
                                )
                        )
                        .toList();

        // 로그인 이후 사용할 서버측 보안정보를 CurrentUser에 담는다.
        return new CurrentUser(
                appUser.getUserId(),
                appUser.getUsername(),
                appUser.getPasswordHash(),
                appUser.getOrganization().getOrganizationId(),
                appUser.getDepartment().getDepartmentId(),
                appUser.getSecurityLevel(),
                appUser.isEnabled(),
                !appUser.isAccountLocked(),
                authorities
        );
    }
}