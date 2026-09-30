-- =============================================================================
-- 파일명 : V22__refine_ai_query_audit_logs.sql
-- =============================================================================
-- 목적
--  - 일반 Database RAG Audit에 사용자 질문과 논리적 데이터 출처를 추가한다.
--  - 기존 Audit Row는 Migration 시 기본값으로 안전하게 보정한다.
--  - 일반 Audit에서 실제 Query 정보인 parameterized_sql을 제거한다.
--  - 실제 Query 검증 정보는 Secure Verification Evidence에서 별도 관리한다.
-- =============================================================================

-- 기존 Row에는 legacy 값을 채우면서 NOT NULL 컬럼을 한 번에 생성한다.
ALTER TABLE dbo.ai_query_audit_logs
    ADD [question] NVARCHAR(1000) NOT NULL
        CONSTRAINT DF_ai_query_audit_logs_question
        DEFAULT N'[legacy audit]' WITH VALUES;

ALTER TABLE dbo.ai_query_audit_logs
    ADD [source] VARCHAR(100) NOT NULL
        CONSTRAINT DF_ai_query_audit_logs_source
        DEFAULT 'legacy' WITH VALUES;


-- 신규 Audit 저장 시 애플리케이션이 반드시 값을 전달하도록
-- Migration용 DEFAULT 제약조건은 제거한다.
ALTER TABLE dbo.ai_query_audit_logs
DROP CONSTRAINT DF_ai_query_audit_logs_question;

ALTER TABLE dbo.ai_query_audit_logs
DROP CONSTRAINT DF_ai_query_audit_logs_source;


-- 실제 SQL은 일반 Audit에 저장하지 않는다.
ALTER TABLE dbo.ai_query_audit_logs
DROP COLUMN parameterized_sql;