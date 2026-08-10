/*
 * =============================================================================
 * 파일명 : V2__create_departments.sql
 * =============================================================================
 * 목적
 *  - 조직에 소속되는 부서 정보를 관리한다.
 *  - 로그인 사용자의 업무 데이터 및 문서 접근 범위를 결정하는 기준으로 사용한다.
 */

CREATE TABLE departments
(
    department_id BIGINT IDENTITY(1,1) NOT NULL,
    organization_id BIGINT NOT NULL,
    department_code VARCHAR(50) NOT NULL,
    department_name NVARCHAR(100) NOT NULL,
    enabled BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT pk_departments
        PRIMARY KEY (department_id),

    CONSTRAINT uk_departments_org_code
        UNIQUE (organization_id, department_code),

    CONSTRAINT fk_departments_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations (organization_id)
);