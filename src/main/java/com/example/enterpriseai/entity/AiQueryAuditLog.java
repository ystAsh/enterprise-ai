/*
 * =============================================================================
 * 클래스명 : AiQueryAuditLog
 * =============================================================================
 * 목적
 *  - Database RAG에서 실행된 Query의 일반 감사 로그를 MSSQL에 저장한다.
 *  - 사용자 질문, Query 식별정보, 실행 결과와 검증 상태를 구조화하여 기록한다.
 *  - 실제 SQL, Binding Parameter, 개인정보, 전체 결과는 일반 Audit에 저장하지 않는다.
 *  - Secure Verification Evidence와 일반 Audit의 책임을 분리한다.
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
            name = "question",
            nullable = false,
            length = 1000
    )
    private String question;

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
            name = "source",
            nullable = false,
            length = 100
    )
    private String source;

    @Column(
            name = "execution_type",
            nullable = false,
            length = 50
    )
    private String executionType;

    @Column(name = "result_count")
    private Long resultCount;

    /*
     * DB 조회 결과의 Java 검증 상태이다.
     *
     * PASSED  : 결과 검증 통과
     * FAILED  : 결과 검증 실패
     * SKIPPED : 검증 이전 종료
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

    // Query 실행 완료 후 일반 감사 로그 객체를 생성한다.
    public static AiQueryAuditLog create(
            String question,
            String queryType,
            String queryKey,
            String source,
            String executionType,
            Long resultCount,
            String validationStatus,
            boolean success,
            long elapsedMs
    ) {
        AiQueryAuditLog log = new AiQueryAuditLog();

        log.question = question;
        log.queryType = queryType;
        log.queryKey = queryKey;
        log.source = source;
        log.executionType = executionType;
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

    public String getQuestion() {
        return question;
    }

    public String getQueryType() {
        return queryType;
    }

    public String getQueryKey() {
        return queryKey;
    }

    public String getSource() {
        return source;
    }

    public String getExecutionType() {
        return executionType;
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