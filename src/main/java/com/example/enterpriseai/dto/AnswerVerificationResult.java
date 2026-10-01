/*
 * =============================================================================
 * 클래스명 : AnswerVerificationResult
 * =============================================================================
 * 목적
 *  - 검증 완료 Evidence와 최종 답변의 사실 일치 검증 결과를 표현한다.
 *  - 질문 단위 전체 상태와 Fact 단위 검증 결과를 함께 관리한다.
 *  - MATCH / MISMATCH / UNSUPPORTED / MISSING 건수를 일관되게 계산한다.
 *  - Match Rate 계산의 원본이 되는 Fact Count를 제공한다.
 *  - 특정 회사, 업무, 필드에 종속되지 않는 공통 검증 결과 모델이다.
 */

package com.example.enterpriseai.dto;

import com.example.enterpriseai.dto.SecureVerificationEvidence.VerificationStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record AnswerVerificationResult(
        VerificationStatus overallStatus,
        List<FactVerification> facts,
        String verifierVersion
) {

    public AnswerVerificationResult {
        if (overallStatus == null) {
            throw new IllegalArgumentException(
                    "Answer Verification 전체 상태가 없습니다."
            );
        }

        facts = facts == null
                ? List.of()
                : List.copyOf(facts);

        if (verifierVersion == null || verifierVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "Answer Verification 버전이 없습니다."
            );
        }

        verifierVersion = verifierVersion.trim();
    }

    public int matchedFactCount() {
        return countByStatus(VerificationStatus.MATCH);
    }

    public int mismatchFactCount() {
        return countByStatus(VerificationStatus.MISMATCH);
    }

    public int unsupportedFactCount() {
        return countByStatus(VerificationStatus.UNSUPPORTED);
    }

    public int missingFactCount() {
        return countByStatus(VerificationStatus.MISSING);
    }

    public int evaluatedFactCount() {
        return facts.size();
    }

    public int comparableFactCount() {
        return matchedFactCount() + mismatchFactCount();
    }

    // 비교 가능한 Fact가 없으면 Match Rate를 임의 생성하지 않는다.
    public BigDecimal matchRate() {
        int comparableFactCount = comparableFactCount();

        if (comparableFactCount == 0) {
            return null;
        }

        return BigDecimal.valueOf(matchedFactCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(comparableFactCount),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private int countByStatus(VerificationStatus status) {
        int count = 0;

        for (FactVerification fact : facts) {
            if (fact.status() == status) {
                count++;
            }
        }

        return count;
    }

    /*
     * 검증 대상 Fact의 식별 정보와 판정 상태만 유지한다.
     * 실제 Evidence 값이나 LLM 답변 값을 평가 결과 모델에 중복 저장하지 않는다.
     */
    public record FactVerification(
            String factReference,
            VerificationStatus status
    ) {

        public FactVerification {
            if (factReference == null || factReference.isBlank()) {
                throw new IllegalArgumentException(
                        "검증 대상 Fact 참조값이 없습니다."
                );
            }

            if (status == null) {
                throw new IllegalArgumentException(
                        "Fact Verification 상태가 없습니다."
                );
            }

            factReference = factReference.trim();
        }
    }
}