-- =============================================================================
-- 파일명 : V26__allow_pending_verification_status.sql
-- =============================================================================
-- 목적
--  - Secure Verification Evidence를 Answer Verification 이전에도 저장할 수 있게 한다.
--  - 아직 평가되지 않은 Evidence에 임의의 Verification Status를 기록하지 않는다.
--  - 실제 검증 완료 후 MATCH / MISMATCH / UNSUPPORTED / MISSING 중 하나를 기록한다.
-- =============================================================================

ALTER TABLE dbo.ai_secure_verification_evidence
ALTER COLUMN verification_status VARCHAR(20) NULL;