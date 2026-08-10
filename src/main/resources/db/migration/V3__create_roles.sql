/*
 * =============================================================================
 * 파일명 : V3__create_roles.sql
 * =============================================================================
 * 목적
 *  - 시스템 기능 권한을 나타내는 역할(Role) 정보를 관리한다.
 *  - Spring Security 인증 이후 사용자의 기능 접근 권한 판단에 사용한다.
 *  - 문서 보안등급(Security Level)과는 별도로 관리한다.
 */

CREATE TABLE roles
(
    role_id BIGINT IDENTITY(1,1) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    role_name NVARCHAR(100) NOT NULL,
    enabled BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT pk_roles
        PRIMARY KEY (role_id),

    CONSTRAINT uk_roles_code
        UNIQUE (role_code)
);