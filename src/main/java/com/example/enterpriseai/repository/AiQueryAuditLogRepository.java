/*
 * =============================================================================
 * 클래스명 : AiQueryAuditLogRepository
 * =============================================================================
 * 목적
 *  - Database RAG Query 실행 감사 로그를 MSSQL에 저장한다.
 *  - ai_query_audit_logs 테이블에 대한 기본 저장/조회 기능을 제공한다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.AiQueryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiQueryAuditLogRepository
        extends JpaRepository<AiQueryAuditLog, Long> {
}