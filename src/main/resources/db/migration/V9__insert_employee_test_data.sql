/*
 * =============================================================================
 * 파일명 : V9__insert_employee_test_data.sql
 * =============================================================================
 * 목적
 *  - Database RAG의 직원 수 조회 기능을 검증하기 위한 테스트 데이터를 구성한다.
 *  - organization_id = 1, department_id = 1 범위의 재직/퇴직 직원을 구분하여 저장한다.
 */

-- 재직 직원 3명
INSERT INTO employees
(
    organization_id,
    department_id,
    employee_name,
    employee_no,
    position_name,
    active
)
VALUES
    (
        1,
        1,
        N'김민수',
        'EMP001',
        N'과장',
        1
    );

INSERT INTO employees
(
    organization_id,
    department_id,
    employee_name,
    employee_no,
    position_name,
    active
)
VALUES
    (
        1,
        1,
        N'이서연',
        'EMP002',
        N'대리',
        1
    );

INSERT INTO employees
(
    organization_id,
    department_id,
    employee_name,
    employee_no,
    position_name,
    active
)
VALUES
    (
        1,
        1,
        N'박준호',
        'EMP003',
        N'사원',
        1
    );

-- 퇴직/비활성 직원 1명
-- 직원 수 조회에서는 active = 1 조건 때문에 제외되어야 한다.
INSERT INTO employees
(
    organization_id,
    department_id,
    employee_name,
    employee_no,
    position_name,
    active
)
VALUES
    (
        1,
        1,
        N'최지훈',
        'EMP004',
        N'대리',
        0
    );