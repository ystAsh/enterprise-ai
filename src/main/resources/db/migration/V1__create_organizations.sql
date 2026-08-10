/*
 * =============================================================================
 * 파일명 : V1__create_organizations.sql
 * =============================================================================
 * 목적
 *  - 시스템에서 사용하는 조직 정보를 관리한다.
 *  - 사용자의 데이터 접근 범위를 결정하는 최상위 조직 기준을 제공한다.
 */

CREATE TABLE organizations
(
    organization_id BIGINT IDENTITY(1,1) NOT NULL,
    organization_code VARCHAR(50) NOT NULL,
    organization_name NVARCHAR(100) NOT NULL,
    enabled BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT pk_organizations
        PRIMARY KEY (organization_id),

    CONSTRAINT uk_organizations_code
        UNIQUE (organization_code)
);