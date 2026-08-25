/*
 * =============================================================================
 * 클래스명 : AiQueryAuditLogService
 * =============================================================================
 * 목적
 *  - Database RAG에서 실행된 Query의 내부 감사 로그를 MSSQL에 저장한다.
 *  - 실제 데이터 조회 Transaction과 감사 로그 저장 Transaction을 분리한다.
 *  - Query 실행 결과와 Java 검증 상태를 공통 감사 정보로 기록한다.
 *  - 특정 업무, 테이블, Query 구현에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.entity.AiQueryAuditLog;
import com.example.enterpriseai.repository.AiQueryAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AiQueryAuditLogService {

    private static final Set<String> ALLOWED_VALIDATION_STATUSES =
            Set.of(
                    "PASSED",
                    "FAILED",
                    "SKIPPED"
            );

    private final AiQueryAuditLogRepository auditLogRepository;

    public AiQueryAuditLogService(
            AiQueryAuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }

    /*
     * Query 실행 결과를 서버 내부 감사 로그로 저장한다.
     *
     * REQUIRES_NEW:
     * 실제 데이터 조회 Transaction과 분리하여
     * 감사 로그를 별도 쓰기 Transaction으로 저장한다.
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
            String validationStatus,
            boolean success,
            long elapsedMs
    ) {

        validate(
                queryType,
                queryKey,
                executionType,
                resultCount,
                validationStatus,
                elapsedMs
        );

        AiQueryAuditLog auditLog =
                AiQueryAuditLog.create(
                        queryType,
                        queryKey,
                        executionType,
                        parameterizedSql,
                        resultCount,
                        validationStatus,
                        success,
                        elapsedMs
                );

        auditLogRepository.save(auditLog);
    }

    /*
     * 감사 로그 저장 전에 최소 공통 조건을 검증한다.
     *
     * 특정 데이터의 업무 의미는 검증하지 않는다.
     */
    private void validate(
            String queryType,
            String queryKey,
            String executionType,
            Long resultCount,
            String validationStatus,
            long elapsedMs
    ) {

        if (queryType == null || queryType.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 유형이 없습니다."
            );
        }

        if (queryKey == null || queryKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 식별자가 없습니다."
            );
        }

        if (executionType == null || executionType.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 실행 방식이 없습니다."
            );
        }

        if (validationStatus == null
                || !ALLOWED_VALIDATION_STATUSES.contains(
                validationStatus
        )) {

            throw new IllegalArgumentException(
                    "허용되지 않은 결과 검증 상태입니다."
            );
        }

        if (resultCount != null && resultCount < 0) {
            throw new IllegalArgumentException(
                    "Query 결과 건수가 올바르지 않습니다."
            );
        }

        if (elapsedMs < 0) {
            throw new IllegalArgumentException(
                    "Query 실행 시간이 올바르지 않습니다."
            );
        }
    }
}