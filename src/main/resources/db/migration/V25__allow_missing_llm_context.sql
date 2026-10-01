-- =============================================================================
-- 파일명 : V25__allow_missing_llm_context.sql
-- =============================================================================
-- 목적
--  - Gemini를 호출하지 않는 실행 경로도 Secure Verification Evidence로 표현한다.
--  - 실제 LLM 호출이 없었던 경우 존재하지 않는 LLM Context를 임의 생성하지 않는다.
-- =============================================================================

ALTER TABLE dbo.ai_secure_verification_evidence
ALTER COLUMN llm_context NVARCHAR(MAX) NULL;