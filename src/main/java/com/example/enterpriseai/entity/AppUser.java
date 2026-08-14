/*
 * =============================================================================
 * 클래스명 : AppUser
 * =============================================================================
 * 목적
 *  - MSSQL app_users 테이블의 로그인 사용자 정보를 표현한다.
 *  - 사용자의 조직, 부서, 보안등급, 계정 상태를 관리한다.
 *  - user_roles를 통해 사용자의 Spring Security Role을 관리한다.
 */

package com.example.enterpriseai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// 이 클래스를 JPA가 관리하는 DB Entity로 지정한다.
@Entity

// 이 Entity가 MSSQL의 app_users 테이블과 연결됨을 지정한다.
// username에는 DB UNIQUE 제약조건을 적용한다.
@Table(
        name = "app_users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_app_users_username",
                        columnNames = "username"
                )
        }
)
public class AppUser {

    // 이 필드를 테이블의 Primary Key로 사용한다.
    @Id

    // MSSQL IDENTITY(자동 증가) 값을 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // app_users.user_id 컬럼과 연결한다.
    @Column(name = "user_id")
    private Long userId;

    // app_users.username 컬럼과 연결한다.
    // NULL을 허용하지 않고 최대 길이는 100이다.
    @Column(
            name = "username",
            nullable = false,
            length = 100
    )
    private String username;

    // 평문 비밀번호가 아닌 BCrypt 해시값만 저장한다.
    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    // 여러 사용자가 하나의 조직에 소속되는 관계이다.
    // LAZY는 실제 조직 정보가 필요할 때 조회하도록 한다.
    @ManyToOne(fetch = FetchType.LAZY)

    // app_users.organization_id를 organizations 테이블과 연결한다.
    @JoinColumn(
            name = "organization_id",
            nullable = false
    )
    private Organization organization;

    // 여러 사용자가 하나의 부서에 소속되는 관계이다.
    @ManyToOne(fetch = FetchType.LAZY)

    // app_users.department_id를 departments 테이블과 연결한다.
    @JoinColumn(
            name = "department_id",
            nullable = false
    )
    private Department department;

    // 문서 및 데이터 열람 범위를 결정하는 보안등급이다.
    @Column(
            name = "security_level",
            nullable = false
    )
    private int securityLevel;

    // 계정 사용 가능 여부이다.
    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled;

    // 로그인 실패 등에 따른 계정 잠금 상태이다.
    @Column(
            name = "account_locked",
            nullable = false
    )
    private boolean accountLocked;

    // 연속 로그인 실패 횟수이다.
    @Column(
            name = "failed_login_count",
            nullable = false
    )
    private int failedLoginCount;

    // 마지막 로그인 시간을 저장한다.
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 마지막 비밀번호 변경 시간을 저장한다.
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // 사용자와 Role의 다대다 관계를 정의한다.
    // 실제 연결 정보는 user_roles 중간 테이블에서 관리한다.
    @ManyToMany(fetch = FetchType.LAZY)

    // user_roles.user_id와 user_roles.role_id를 이용해 Role을 연결한다.
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(
                    name = "user_id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id"
            )
    )
    private Set<Role> roles = new HashSet<>();

    // JPA가 Entity 객체를 생성할 때 사용하는 기본 생성자이다.
    protected AppUser() {
    }

    // 사용자 생성에 필요한 값만 받아 정상적인 초기 상태로 만든다.
    public static AppUser create(
            String username,
            String passwordHash,
            Organization organization,
            Department department,
            int securityLevel
    ) {
        AppUser user = new AppUser();

        user.username = username;
        user.passwordHash = passwordHash;
        user.organization = organization;
        user.department = department;
        user.securityLevel = securityLevel;

        user.enabled = true;
        user.accountLocked = false;
        user.failedLoginCount = 0;
        user.passwordChangedAt = LocalDateTime.now();

        return user;
    }

    // 사용자에게 Spring Security 기능 권한을 추가한다.
    public void addRole(Role role) {
        this.roles.add(role);
    }

    // 로그인 성공 시 실패 횟수를 초기화하고 마지막 로그인 시간을 기록한다.
    public void recordLoginSuccess() {
        this.failedLoginCount = 0;
        this.lastLoginAt = LocalDateTime.now();
    }

    // 로그인 실패 횟수를 증가시킨다.
    // 5회 이상 실패하면 계정을 잠근다.
    public void recordLoginFailure() {
        this.failedLoginCount++;

        if (this.failedLoginCount >= 5) {
            this.accountLocked = true;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Department getDepartment() {
        return department;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}