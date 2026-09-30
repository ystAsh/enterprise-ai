-- =============================================================================
-- 파일명 : V23__create_secure_verification_evidence.sql
-- =============================================================================
-- 목적
--  - 개발자/관리자가 AI 답변의 실제 근거를 재검증할 수 있는
--    Secure Verification Evidence를 MSSQL에 저장한다.
--  - 일반 Audit 및 일반 사용자 Evidence와 분리한다.
--  - 저장 정책을 통과한 Parameter, Result, LLM Context, Answer만 저장한다.
--  - 실제 Query는 기존 시스템이 제공하고 정책에서 허용한 경우에만 저장한다.
-- =============================================================================

CREATE TABLE dbo.ai_secure_verification_evidence
(
    verification_evidence_id BIGINT IDENTITY(1,1) NOT NULL,

    question NVARCHAR(1000) NOT NULL,

    source VARCHAR(100) NOT NULL,

    query_key VARCHAR(150) NOT NULL,

    execution_type VARCHAR(50) NOT NULL,

    -- 기존 시스템이 실제 Query를 제공하고
    -- 서버 정책에서 저장을 허용한 경우에만 저장한다.
    actual_query NVARCHAR(MAX) NULL,

    -- 실제 Query를 직접 저장하지 않는 경우
    -- 기존 시스템의 실행 로그 ID 등으로 연결한다.
    query_reference VARCHAR(500) NULL,

    query_version VARCHAR(100) NULL,

    query_hash VARCHAR(128) NULL,

    -- Sanitizer를 통과한 허용/마스킹 Parameter만 JSON으로 저장한다.
    stored_parameters_json NVARCHAR(MAX) NULL,

    -- SNAPSHOT 정책일 때만 검증 완료 소량 Result를 JSON으로 저장한다.
    validated_result_json NVARCHAR(MAX) NULL,

    result_count BIGINT NOT NULL,

    -- 대량 Result는 전체 Rows 대신 참조정보/Hash로 연결한다.
    result_reference VARCHAR(500) NULL,

    result_hash VARCHAR(128) NULL,

    -- 실제 Gemini에 전달한 검증 완료 최소 Context
    llm_context NVARCHAR(MAX) NOT NULL,

    -- 실제 최종 AI 답변
    final_answer NVARCHAR(MAX) NOT NULL,

    verification_status VARCHAR(20) NOT NULL,

    executed_at DATETIME2 NOT NULL,

    -- Evidence 보관 정책 만료 시각
    expires_at DATETIME2 NOT NULL,

    CONSTRAINT pk_ai_secure_verification_evidence
        PRIMARY KEY (verification_evidence_id),

    CONSTRAINT ck_secure_verification_result_count
        CHECK (result_count >= 0),

    CONSTRAINT ck_secure_verification_status
        CHECK (
            verification_status IN (
                                    'MATCH',
                                    'MISMATCH',
                                    'UNSUPPORTED',
                                    'MISSING'
                )
            ),

    CONSTRAINT ck_secure_verification_query_evidence
        CHECK (
            actual_query IS NOT NULL
                OR query_reference IS NOT NULL
            ),

    CONSTRAINT ck_secure_verification_result_evidence
        CHECK (
            validated_result_json IS NOT NULL
                OR result_reference IS NOT NULL
                OR result_hash IS NOT NULL
            ),

    CONSTRAINT ck_secure_verification_parameters_json
        CHECK (
            stored_parameters_json IS NULL
                OR ISJSON(stored_parameters_json) = 1
            ),

    CONSTRAINT ck_secure_verification_result_json
        CHECK (
            validated_result_json IS NULL
                OR ISJSON(validated_result_json) = 1
            )
);


CREATE INDEX ix_secure_verification_query_key_executed_at
    ON dbo.ai_secure_verification_evidence (
                                            query_key,
                                            executed_at
        );


CREATE INDEX ix_secure_verification_status_executed_at
    ON dbo.ai_secure_verification_evidence (
                                            verification_status,
                                            executed_at
        );


CREATE INDEX ix_secure_verification_expires_at
    ON dbo.ai_secure_verification_evidence (
                                            expires_at
        );