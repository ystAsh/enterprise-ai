/*
=============================================================================
파일명 : V16__insert_sample_security_scope_data.sql
=============================================================================
목적
 - 부서 권한 범위 검증을 위한 추가 업무 데이터를 생성한다.
 - 기존 로그인 사용자의 DEPT001 외 다른 부서 데이터를 만든다.
=============================================================================
*/

INSERT INTO dbo.departments
(
    organization_id,
    department_code,
    department_name,
    enabled
)
SELECT
    organization_id,
    'DEPT002',
    N'영업부서',
    1
FROM dbo.organizations
WHERE organization_code = 'ORG001';


INSERT INTO dbo.employees
(
    organization_id,
    department_id,
    employee_name,
    employee_no,
    position_name,
    active
)
SELECT
    o.organization_id,
    d.department_id,
    N'박지훈',
    'EMP101',
    N'대리',
    1
FROM dbo.organizations o
         JOIN dbo.departments d
              ON d.organization_id = o.organization_id
WHERE o.organization_code = 'ORG001'
  AND d.department_code = 'DEPT002';


INSERT INTO dbo.employees
(
    organization_id,
    department_id,
    employee_name,
    employee_no,
    position_name,
    active
)
SELECT
    o.organization_id,
    d.department_id,
    N'이서연',
    'EMP102',
    N'과장',
    1
FROM dbo.organizations o
         JOIN dbo.departments d
              ON d.organization_id = o.organization_id
WHERE o.organization_code = 'ORG001'
  AND d.department_code = 'DEPT002';