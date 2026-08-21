/*
 * =============================================================================
 * 파일명 : V8__create_employees.sql
 * =============================================================================
 * 목적
 *  - Database RAG 테스트에 사용할 직원 업무 데이터를 MSSQL에 구성한다.
 *  - 직원의 조직 및 부서 범위를 서버에서 안전하게 조회할 수 있도록 한다.
 */

CREATE TABLE employees
(
    employee_id BIGINT IDENTITY(1,1) NOT NULL,

    organization_id BIGINT NOT NULL,

    department_id BIGINT NOT NULL,

    employee_name NVARCHAR(100) NOT NULL,

    employee_no VARCHAR(30) NOT NULL,

    position_name NVARCHAR(100) NULL,

    active BIT NOT NULL DEFAULT 1,

    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT pk_employees
        PRIMARY KEY (employee_id),

    CONSTRAINT uq_employees_employee_no
        UNIQUE (employee_no),

    CONSTRAINT fk_employees_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations (organization_id),

    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id)
            REFERENCES departments (department_id)
);

CREATE INDEX ix_employees_security_scope
    ON employees (
                  organization_id,
                  department_id,
                  active
        );