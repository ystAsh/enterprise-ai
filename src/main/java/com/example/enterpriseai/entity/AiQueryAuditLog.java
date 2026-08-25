/*
 * =============================================================================
 * 클래스명 : AiQueryAuditLog
 * =============================================================================
 * 목적
 *  - Database RAG에서 실행된 Query의 내부 감사 로그를 MSSQL에 저장한다.
 *  - 사용자 evidence와 분리하여 Query 실행 및 검증 이력을 서버 내부에서 추적한다.
 *  - 특정 업무, 테이블, Query 구현에 종속되지 않는다.
 */

package com.example.enterpriseai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_query_audit_logs")
public class AiQueryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    @Column(
            name = "query_type",
            nullable = false,
            length = 100
    )
    private String queryType;

    @Column(
            name = "query_key",
            nullable = false,
            length = 150
    )
    private String queryKey;

    @Column(
            name = "execution_type",
            nullable = false,
            length = 50
    )
    private String executionType;

    /*
     * JdbcClient / Query Builder에서 직접 생성한
     * parameterized SQL만 필요한 경우 저장한다.
     *
     * 실제 값이 치환된 SQL은 저장하지 않는다.
     * JPA Repository Query에서는 null로 둔다.
     */
    @Column(name = "parameterized_sql")
    private String parameterizedSql;

    @Column(name = "result_count")
    private Long resultCount;

    /*
     * DB 조회 결과의 Java 검증 상태이다.
     *
     * PASSED  : 결과 검증 통과
     * FAILED  : 결과 검증 실패
     * SKIPPED : 검증 이전 종료 또는 기존 감사 로그
     */
    @Column(
            name = "validation_status",
            nullable = false,
            length = 20
    )
    private String validationStatus;

    @Column(
            name = "success",
            nullable = false
    )
    private boolean success;

    @Column(
            name = "elapsed_ms",
            nullable = false
    )
    private long elapsedMs;

    @Column(
            name = "executed_at",
            nullable = false
    )
    private LocalDateTime executedAt;

    protected AiQueryAuditLog() {
    }

    /*
     * Query 실행 완료 후 감사 로그 객체를 생성한다.
     */
    public static AiQueryAuditLog create(
            String queryType,
            String queryKey,
            String executionType,
            String parameterizedSql,
            Long resultCount,
            String validationStatus,
            boolean success,
            long elapsedMs
    ) {

        AiQueryAuditLog log =
                new AiQueryAuditLog();

        log.queryType = queryType;
        log.queryKey = queryKey;
        log.executionType = executionType;
        log.parameterizedSql = parameterizedSql;
        log.resultCount = resultCount;
        log.validationStatus = validationStatus;
        log.success = success;
        log.elapsedMs = elapsedMs;
        log.executedAt = LocalDateTime.now();

        return log;
    }

    public Long getAuditLogId() {
        return auditLogId;
    }

    public String getQueryType() {
        return queryType;
    }

    public String getQueryKey() {
        return queryKey;
    }

    public String getExecutionType() {
        return executionType;
    }

    public String getParameterizedSql() {
        return parameterizedSql;
    }

    public Long getResultCount() {
        return resultCount;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
}