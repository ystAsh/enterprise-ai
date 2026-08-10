/*
 * =============================================================================
 * 파일명 : V6__insert_initial_security_data.sql
 * =============================================================================
 * 목적
 *  - 로그인 테스트에 필요한 최소 조직/부서/Role 정보를 생성한다.
 *  - 실제 사용자 비밀번호는 평문으로 저장하지 않고 BCrypt 해시만 저장한다.
 */

INSERT INTO organizations (
    organization_code,
    organization_name,
    enabled
)
VALUES (
           'ORG001',
           N'본사',
           1
       );

INSERT INTO departments (
    organization_id,
    department_code,
    department_name,
    enabled
)
SELECT
    organization_id,
    'DEPT001',
    N'IT부서',
    1
FROM organizations
WHERE organization_code = 'ORG001';

INSERT INTO roles (
    role_code,
    role_name,
    enabled
)
VALUES
    ('ROLE_USER', N'일반 사용자', 1),
    ('ROLE_MANAGER', N'관리자', 1),
    ('ROLE_ADMIN', N'시스템 관리자', 1);