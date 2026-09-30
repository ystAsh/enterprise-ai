-- =============================================================================
-- 파일명 : V24__allow_missing_query_evidence.sql
-- =============================================================================
-- 목적
--  - 기존 시스템이 실제 Query 또는 Query Reference를 제공하지 않는 경우도
--    Secure Verification Evidence로 정확하게 표현할 수 있도록 한다.
--  - enterprise-ai가 존재하지 않는 Query Evidence를 임의 생성하지 않는다.
-- =============================================================================

ALTER TABLE dbo.ai_secure_verification_evidence
DROP CONSTRAINT ck_secure_verification_query_evidence;