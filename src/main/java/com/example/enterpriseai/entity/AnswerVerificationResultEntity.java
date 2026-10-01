/*
 * =============================================================================
 * 클래스명 : AnswerVerificationResultEntity
 * =============================================================================
 * 목적
 *  - Answer Verification 질문 단위 평가 결과를 MSSQL에 영속화한다.
 *  - Secure Verification Evidence와 ID로만 연결한다.
 *  - Fact Count, Match Rate, Verifier Version, 평가 시각을 구조화하여 저장한다.
 *  - Raw SQL, Raw Result, 실제 Parameter, LLM Context를 중복 저장하지 않는다.
 */

package com.example.enterpriseai.entity;

import com.example.enterpriseai.dto.AnswerVerificationResult;
import com.example.enterpriseai.dto.SecureVerificationEvidence.VerificationStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_answer_verification_result", schema = "dbo")
public class AnswerVerificationResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_verification_result_id")
    private Long id;

    @Column(name = "verification_evidence_id", nullable = false)
    private Long verificationEvidenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 20)
    private VerificationStatus overallStatus;

    @Column(name = "matched_fact_count", nullable = false)
    private int matchedFactCount;

    @Column(name = "mismatch_fact_count", nullable = false)
    private int mismatchFactCount;

    @Column(name = "unsupported_fact_count", nullable = false)
    private int unsupportedFactCount;

    @Column(name = "missing_fact_count", nullable = false)
    private int missingFactCount;

    @Column(name = "evaluated_fact_count", nullable = false)
    private int evaluatedFactCount;

    @Column(name = "match_rate", precision = 5, scale = 2)
    private BigDecimal matchRate;

    @Column(name = "verifier_version", nullable = false, length = 100)
    private String verifierVersion;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    protected AnswerVerificationResultEntity() {
    }

    public static AnswerVerificationResultEntity create(
            Long verificationEvidenceId,
            AnswerVerificationResult result,
            LocalDateTime verifiedAt
    ) {
        if (verificationEvidenceId == null || verificationEvidenceId <= 0) {
            throw new IllegalArgumentException(
                    "Secure Verification Evidence ID가 올바르지 않습니다."
            );
        }

        if (result == null) {
            throw new IllegalArgumentException(
                    "Answer Verification 결과가 없습니다."
            );
        }

        if (verifiedAt == null) {
            throw new IllegalArgumentException(
                    "Answer Verification 평가 시각이 없습니다."
            );
        }

        AnswerVerificationResultEntity entity =
                new AnswerVerificationResultEntity();

        entity.verificationEvidenceId = verificationEvidenceId;
        entity.overallStatus = result.overallStatus();
        entity.matchedFactCount = result.matchedFactCount();
        entity.mismatchFactCount = result.mismatchFactCount();
        entity.unsupportedFactCount = result.unsupportedFactCount();
        entity.missingFactCount = result.missingFactCount();
        entity.evaluatedFactCount = result.evaluatedFactCount();
        entity.matchRate = result.matchRate();
        entity.verifierVersion = result.verifierVersion();
        entity.verifiedAt = verifiedAt;

        return entity;
    }

    public Long getId() {
        return id;
    }

    public Long getVerificationEvidenceId() {
        return verificationEvidenceId;
    }

    public VerificationStatus getOverallStatus() {
        return overallStatus;
    }

    public int getMatchedFactCount() {
        return matchedFactCount;
    }

    public int getMismatchFactCount() {
        return mismatchFactCount;
    }

    public int getUnsupportedFactCount() {
        return unsupportedFactCount;
    }

    public int getMissingFactCount() {
        return missingFactCount;
    }

    public int getEvaluatedFactCount() {
        return evaluatedFactCount;
    }

    public BigDecimal getMatchRate() {
        return matchRate;
    }

    public String getVerifierVersion() {
        return verifierVersion;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
}