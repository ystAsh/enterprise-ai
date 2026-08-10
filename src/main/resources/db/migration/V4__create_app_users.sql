/*
 * =============================================================================
 * 파일명 : V4__create_app_users.sql
 * =============================================================================
 * 목적
 *  - Spring Security 로그인에 사용할 사용자 정보를 관리한다.
 *  - 사용자 조직, 부서, 보안등급, 계정 상태를 함께 관리한다.
 *  - 이후 Document RAG와 Database RAG의 접근 범위 기준으로 사용한다.
 */

CREATE TABLE app_users
(
    user_id BIGINT IDENTITY(1,1) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    organization_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,

    security_level INT NOT NULL DEFAULT 1,

    enabled BIT NOT NULL DEFAULT 1,
    account_locked BIT NOT NULL DEFAULT 0,
    failed_login_count INT NOT NULL DEFAULT 0,

    last_login_at DATETIME2 NULL,
    password_changed_at DATETIME2 NULL,

    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT pk_app_users
        PRIMARY KEY (user_id),

    CONSTRAINT uk_app_users_username
        UNIQUE (username),

    CONSTRAINT fk_app_users_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations (organization_id),

    CONSTRAINT fk_app_users_department
        FOREIGN KEY (department_id)
            REFERENCES departments (department_id),

    CONSTRAINT ck_app_users_security_level
        CHECK (security_level BETWEEN 1 AND 5),

    CONSTRAINT ck_app_users_failed_login_count
        CHECK (failed_login_count >= 0)
);