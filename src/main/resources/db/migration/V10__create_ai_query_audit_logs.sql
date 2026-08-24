/*
 * =============================================================================
 * 파일명 : V10__create_ai_query_audit_logs.sql
 * =============================================================================
 * 목적
 *  - Database RAG에서 실행된 업무 Query의 내부 감사 이력을 MSSQL에 저장한다.
 *  - 사용자에게 반환하는 evidence와 서버 내부 Audit Log를 분리한다.
 *  - 실제 파라미터 값이나 개인정보는 저장하지 않는다.
 */

CREATE TABLE ai_query_audit_logs
(
    audit_log_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 업무 Query 유형
    -- 예: EMPLOYEE_COUNT
    query_type VARCHAR(100) NOT NULL,

    -- 서버 내부 Query 식별자
    -- 예: EMPLOYEE_COUNT_BY_CURRENT_SCOPE
    query_key VARCHAR(150) NOT NULL,

    -- 실행 기술
    -- 예: JPA_REPOSITORY / MYBATIS / JDBC_CLIENT
    execution_type VARCHAR(50) NOT NULL,

    /*
     * Java Query Builder / JdbcClient에서 직접 생성한
     * parameterized SQL만 필요 시 기록한다.
     *
     * JPA Repository 실행에서는 NULL로 둔다.
     */
    parameterized_sql NVARCHAR(MAX) NULL,

    -- 조회 결과 건수 또는 집계 결과 추적용 값
    result_count BIGINT NULL,

    -- Query 실행 성공 여부
    success BIT NOT NULL,

    -- Query 실행 소요 시간
    elapsed_ms BIGINT NOT NULL,

    -- Query 실행 시각
    executed_at DATETIME2 NOT NULL,

    CONSTRAINT pk_ai_query_audit_logs
        PRIMARY KEY (audit_log_id)
);


-- queryKey와 실행 시각을 기준으로 감사 로그를 조회할 때 사용한다.
CREATE INDEX ix_ai_query_audit_logs_query_key_executed_at
    ON ai_query_audit_logs (
                            query_key,
                            executed_at
        );


-- 실패 Query 추적에 사용한다.
CREATE INDEX ix_ai_query_audit_logs_success_executed_at
    ON ai_query_audit_logs (
                            success,
                            executed_at
        );