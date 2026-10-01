/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidenceEntity
 * =============================================================================
 * 목적
 *  - Secure Verification Evidence를 MSSQL에 영속화하기 위한 JPA Entity이다.
 *  - 일반 Audit 및 일반 사용자 Evidence와 분리하여 관리한다.
 *  - Policy와 Sanitizer를 통과한 검증 정보만 저장 대상으로 사용한다.
 *  - 실제 Query, Parameter, Result의 저장 여부를 이 Entity가 판단하지 않는다.
 *  - LLM 미사용 및 Answer Verification 전 상태를 nullable 컬럼으로 표현한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_secure_verification_evidence",
        schema = "dbo"
)
public class SecureVerificationEvidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_evidence_id")
    private Long verificationEvidenceId;

    @Column(name = "question", nullable = false, length = 1000)
    private String question;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "query_key", nullable = false, length = 150)
    private String queryKey;

    @Column(name = "execution_type", nullable = false, length = 50)
    private String executionType;

    @Column(name = "actual_query", columnDefinition = "nvarchar(max)")
    private String actualQuery;

    @Column(name = "query_reference", length = 500)
    private String queryReference;

    @Column(name = "query_version", length = 100)
    private String queryVersion;

    @Column(name = "query_hash", length = 128)
    private String queryHash;

    @Column(name = "stored_parameters_json", columnDefinition = "nvarchar(max)")
    private String storedParametersJson;

    @Column(name = "validated_result_json", columnDefinition = "nvarchar(max)")
    private String validatedResultJson;

    @Column(name = "result_count", nullable = false)
    private long resultCount;

    @Column(name = "result_reference", length = 500)
    private String resultReference;

    @Column(name = "result_hash", length = 128)
    private String resultHash;

    @Column(name = "llm_context", columnDefinition = "nvarchar(max)")
    private String llmContext;

    @Column(name = "final_answer", nullable = false, columnDefinition = "nvarchar(max)")
    private String finalAnswer;

    @Column(name = "verification_status", length = 20)
    private String verificationStatus;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected SecureVerificationEvidenceEntity() {
    }

    // Sanitizer와 저장 정책을 통과한 값으로 영속화 Entity를 생성한다.
    public static SecureVerificationEvidenceEntity create(
            String question,
            String source,
            String queryKey,
            String executionType,
            String actualQuery,
            String queryReference,
            String queryVersion,
            String queryHash,
            String storedParametersJson,
            String validatedResultJson,
            long resultCount,
            String resultReference,
            String resultHash,
            String llmContext,
            String finalAnswer,
            String verificationStatus,
            LocalDateTime executedAt,
            LocalDateTime expiresAt
    ) {
        SecureVerificationEvidenceEntity entity =
                new SecureVerificationEvidenceEntity();

        entity.question = question;
        entity.source = source;
        entity.queryKey = queryKey;
        entity.executionType = executionType;
        entity.actualQuery = actualQuery;
        entity.queryReference = queryReference;
        entity.queryVersion = queryVersion;
        entity.queryHash = queryHash;
        entity.storedParametersJson = storedParametersJson;
        entity.validatedResultJson = validatedResultJson;
        entity.resultCount = resultCount;
        entity.resultReference = resultReference;
        entity.resultHash = resultHash;
        entity.llmContext = llmContext;
        entity.finalAnswer = finalAnswer;
        entity.verificationStatus = verificationStatus;
        entity.executedAt = executedAt;
        entity.expiresAt = expiresAt;

        return entity;
    }

    public Long getVerificationEvidenceId() {
        return verificationEvidenceId;
    }

    public String getQuestion() {
        return question;
    }

    public String getSource() {
        return source;
    }

    public String getQueryKey() {
        return queryKey;
    }

    public String getExecutionType() {
        return executionType;
    }

    public String getActualQuery() {
        return actualQuery;
    }

    public String getQueryReference() {
        return queryReference;
    }

    public String getQueryVersion() {
        return queryVersion;
    }

    public String getQueryHash() {
        return queryHash;
    }

    public String getStoredParametersJson() {
        return storedParametersJson;
    }

    public String getValidatedResultJson() {
        return validatedResultJson;
    }

    public long getResultCount() {
        return resultCount;
    }

    public String getResultReference() {
        return resultReference;
    }

    public String getResultHash() {
        return resultHash;
    }

    public String getLlmContext() {
        return llmContext;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}