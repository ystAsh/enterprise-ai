/*
=============================================================================
파일명 : V11__add_validation_status_to_ai_query_audit_logs.sql
=============================================================================
목적
 - Database RAG Query 실행 결과의 검증 상태를 감사 로그에 기록한다.
 - 기존 V10 Migration을 수정하지 않고 신규 Migration으로 확장한다.
=============================================================================
*/

ALTER TABLE dbo.ai_query_audit_logs
    ADD validation_status varchar(20) NOT NULL
    CONSTRAINT DF_ai_query_audit_logs_validation_status
    DEFAULT 'SKIPPED';