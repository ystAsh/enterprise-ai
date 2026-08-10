/*
 * =============================================================================
 * 클래스명 : UserRegistrationService
 * =============================================================================
 * 목적
 *  - 신규 로그인 사용자를 생성한다.
 *  - 비밀번호를 BCrypt로 암호화한 뒤 MSSQL에 저장한다.
 *  - 조직, 부서, Role을 DB 기준으로 검증한 후 사용자에게 연결한다.
 */

package com.example.enterpriseai.service.security;

import com.example.enterpriseai.entity.AppUser;
import com.example.enterpriseai.entity.Department;
import com.example.enterpriseai.entity.Organization;
import com.example.enterpriseai.entity.Role;
import com.example.enterpriseai.repository.AppUserRepository;
import com.example.enterpriseai.repository.DepartmentRepository;
import com.example.enterpriseai.repository.OrganizationRepository;
import com.example.enterpriseai.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            AppUserRepository appUserRepository,
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 테스트 로그인에 사용할 최소 사용자 계정을 생성한다.
    @Transactional
    public void createTestUser(
            String username,
            String rawPassword
    ) {
        // 동일한 username이 이미 존재하면 중복 생성하지 않는다.
        if (appUserRepository.existsByUsername(username)) {
            return;
        }

        // 사용자가 속할 활성 조직을 DB에서 조회한다.
        Organization organization =
                organizationRepository
                        .findByOrganizationCodeAndEnabledTrue("ORG001")
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "ORG001 조직이 존재하지 않습니다."
                                )
                        );

        // 조직에 속한 활성 부서를 DB에서 조회한다.
        Department department =
                departmentRepository
                        .findByOrganizationOrganizationIdAndDepartmentCodeAndEnabledTrue(
                                organization.getOrganizationId(),
                                "DEPT001"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "DEPT001 부서가 존재하지 않습니다."
                                )
                        );

        // 일반 사용자 Role을 DB에서 조회한다.
        Role role =
                roleRepository
                        .findByRoleCodeAndEnabledTrue("ROLE_USER")
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "ROLE_USER가 존재하지 않습니다."
                                )
                        );

        // 평문 비밀번호를 BCrypt 해시로 변환한다.
        String passwordHash =
                passwordEncoder.encode(rawPassword);

        // 검증된 조직/부서/보안등급으로 사용자 객체를 생성한다.
        AppUser user =
                AppUser.create(
                        username,
                        passwordHash,
                        organization,
                        department,
                        3
                );

        // ROLE_USER 권한을 사용자에게 부여한다.
        user.addRole(role);

        // app_users와 user_roles 정보를 하나의 트랜잭션으로 저장한다.
        appUserRepository.save(user);
    }
}