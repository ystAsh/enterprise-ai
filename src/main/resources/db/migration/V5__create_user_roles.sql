/*
 * =============================================================================
 * 파일명 : V5__create_user_roles.sql
 * =============================================================================
 * 목적
 *  - 사용자와 역할(Role)의 다대다 관계를 관리한다.
 *  - Spring Security에서 사용자의 기능 접근 권한을 조회하는 기준으로 사용한다.
 *  - 보안등급(Security Level)과는 별도로 관리한다.
 */

CREATE TABLE user_roles
(
    user_role_id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    -- 동일 사용자에게 동일 역할이 중복 등록되지 않도록 한다.
    CONSTRAINT pk_user_roles
        PRIMARY KEY (user_role_id),

    CONSTRAINT uk_user_roles_user_role
        UNIQUE (user_id, role_id),

    -- 사용자 정보와 연결한다.
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
            REFERENCES app_users (user_id),

    -- 역할 정보와 연결한다.
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
            REFERENCES roles (role_id)
);