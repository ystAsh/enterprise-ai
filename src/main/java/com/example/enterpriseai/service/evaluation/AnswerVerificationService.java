/*
 * =============================================================================
 * 클래스명 : AnswerVerificationService
 * =============================================================================
 * 목적
 *  - 검증 완료 Evidence와 최종 답변을 Java deterministic 방식으로 비교한다.
 *  - Evidence의 scalar Fact 값이 실제 답변에 존재하는지 확인한다.
 *  - 필수 Fact가 답변에 없으면 MISSING으로 판정한다.
 *  - Evidence로 확인 가능한 Fact가 전혀 없으면 UNSUPPORTED로 보수적으로 판정한다.
 *  - 업무 의미나 필드 의미를 추측하여 MATCH/MISMATCH를 생성하지 않는다.
 *  - 특정 회사, 업무, 필드에 종속되지 않는 공통 Answer Verification을 제공한다.
 */

package com.example.enterpriseai.service.evaluation;

import com.example.enterpriseai.dto.AnswerVerificationPolicy;
import com.example.enterpriseai.dto.AnswerVerificationResult;
import com.example.enterpriseai.dto.AnswerVerificationResult.FactVerification;
import com.example.enterpriseai.dto.SecureVerificationEvidence.VerificationStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AnswerVerificationService {

    private static final String VERIFIER_VERSION = "deterministic-scalar-v1";

    private final AnswerVerificationFactExtractor factExtractor;

    public AnswerVerificationService(
            AnswerVerificationFactExtractor factExtractor
    ) {
        this.factExtractor = factExtractor;
    }

    // 검증 완료 Result의 scalar Fact와 실제 최종 답변을 deterministic 방식으로 비교한다.
    public AnswerVerificationResult verify(
            Map<String, Object> validatedResult,
            String finalAnswer,
            AnswerVerificationPolicy policy
    ) {
        if (finalAnswer == null || finalAnswer.isBlank()) {
            throw new IllegalArgumentException(
                    "Answer Verification 대상 최종 답변이 없습니다."
            );
        }

        if (policy == null) {
            throw new IllegalArgumentException(
                    "Answer Verification 정책이 없습니다."
            );
        }

        List<AnswerVerificationFactExtractor.FactCandidate> candidates =
                factExtractor.extract(validatedResult);

        String normalizedAnswer =
                normalizeText(finalAnswer);

        List<FactVerification> facts =
                new ArrayList<>();

        int matchedCount = 0;
        int missingCount = 0;

        for (AnswerVerificationFactExtractor.FactCandidate candidate : candidates) {
            if (containsValue(
                    normalizedAnswer,
                    candidate.comparableValue()
            )) {
                facts.add(
                        new FactVerification(
                                candidate.factReference(),
                                VerificationStatus.MATCH
                        )
                );

                matchedCount++;
                continue;
            }

            if (policy.isRequired(candidate.factReference())) {
                facts.add(
                        new FactVerification(
                                candidate.factReference(),
                                VerificationStatus.MISSING
                        )
                );

                missingCount++;
            }
        }

        VerificationStatus overallStatus =
                determineOverallStatus(
                        candidates,
                        matchedCount,
                        missingCount
                );

        return new AnswerVerificationResult(
                overallStatus,
                facts,
                VERIFIER_VERSION
        );
    }

    private VerificationStatus determineOverallStatus(
            List<AnswerVerificationFactExtractor.FactCandidate> candidates,
            int matchedCount,
            int missingCount
    ) {
        if (missingCount > 0) {
            return VerificationStatus.MISSING;
        }

        if (matchedCount > 0) {
            return VerificationStatus.MATCH;
        }

        if (!candidates.isEmpty()) {
            return VerificationStatus.UNSUPPORTED;
        }

        return VerificationStatus.UNSUPPORTED;
    }

    private boolean containsValue(
            String normalizedAnswer,
            String comparableValue
    ) {
        String normalizedValue =
                normalizeText(comparableValue);

        if (normalizedValue.isBlank()) {
            return false;
        }

        return normalizedAnswer.contains(
                normalizedValue
        );
    }

    private String normalizeText(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}