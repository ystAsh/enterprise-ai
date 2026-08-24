/*
 * =============================================================================
 * 클래스명 : AiQueryAuditLogService
 * =============================================================================
 * 목적
 *  - Database RAG에서 실행된 업무 Query의 감사 로그를 MSSQL에 저장한다.
 *  - 실제 업무 조회 Transaction과 감사 로그 저장 Transaction을 분리한다.
 *  - JPA Query에서는 SQL을 복제하지 않고 queryKey 중심으로 기록한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.entity.AiQueryAuditLog;
import com.example.enterpriseai.repository.AiQueryAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiQueryAuditLogService {

    private final AiQueryAuditLogRepository auditLogRepository;

    public AiQueryAuditLogService(
            AiQueryAuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }

    /*
     * Query 실행 결과를 내부 감사 로그로 저장한다.
     *
     * REQUIRES_NEW:
     * 업무 조회가 readOnly Transaction이어도
     * Audit Log는 별도의 쓰기 Transaction에서 저장한다.
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void save(
            String queryType,
            String queryKey,
            String executionType,
            String parameterizedSql,
            Long resultCount,
            boolean success,
            long elapsedMs
    ) {

        AiQueryAuditLog auditLog =
                AiQueryAuditLog.create(
                        queryType,
                        queryKey,
                        executionType,
                        parameterizedSql,
                        resultCount,
                        success,
                        elapsedMs
                );

        auditLogRepository.save(auditLog);
    }
}