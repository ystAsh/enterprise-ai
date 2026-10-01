/*
 * =============================================================================
 * 클래스명 : AnswerVerificationService
 * =============================================================================
 * 목적
 *  - 검증 완료 Evidence와 최종 답변을 Java deterministic 방식으로 비교한다.
 *  - Evidence의 scalar Fact 값이 실제 답변에 존재하는지 확인한다.
 *  - 필수 Fact가 답변에 없으면 MISSING으로 판정한다.
 *  - Evidence로 확인 가능한 Fact가 전혀 없으면 UNSUPPORTED로 보수적으로 판정한다.
 *  - 숫자/Boolean 값의 부분 문자열 오탐을 방지한다.
 *  - 한국어 조사 등이 숫자 바로 뒤에 붙는 자연어 표현은 정상 MATCH로 허용한다.
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
import java.util.regex.Pattern;

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
            if (containsExactScalarValue(
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
            int matchedCount,
            int missingCount
    ) {
        if (missingCount > 0) {
            return VerificationStatus.MISSING;
        }

        if (matchedCount > 0) {
            return VerificationStatus.MATCH;
        }

        return VerificationStatus.UNSUPPORTED;
    }

    private boolean containsExactScalarValue(
            String normalizedAnswer,
            String comparableValue
    ) {
        String normalizedValue =
                normalizeText(comparableValue);

        if (normalizedValue.isBlank()) {
            return false;
        }

        if (isNumeric(normalizedValue)) {
            return containsNumericValue(
                    normalizedAnswer,
                    normalizedValue
            );
        }

        if (isBoolean(normalizedValue)) {
            return containsBooleanValue(
                    normalizedAnswer,
                    normalizedValue
            );
        }

        return normalizedAnswer.contains(
                normalizedValue
        );
    }

    // 3은 "3입니다", "3건"에는 일치하지만 31, 3.1, 3,000 등의 일부로는 일치하지 않는다.
    private boolean containsNumericValue(
            String normalizedAnswer,
            String normalizedValue
    ) {
        Pattern pattern =
                Pattern.compile(
                        "(?<![\\p{N}+\\-.,])"
                                + Pattern.quote(normalizedValue)
                                + "(?![\\p{N}.,])"
                );

        return pattern.matcher(normalizedAnswer).find();
    }

    // Boolean은 다른 영문/숫자 토큰 내부에 포함된 경우 MATCH하지 않는다.
    private boolean containsBooleanValue(
            String normalizedAnswer,
            String normalizedValue
    ) {
        Pattern pattern =
                Pattern.compile(
                        "(?<![\\p{L}\\p{N}])"
                                + Pattern.quote(normalizedValue)
                                + "(?![\\p{L}\\p{N}])"
                );

        return pattern.matcher(normalizedAnswer).find();
    }

    private boolean isNumeric(String value) {
        return value.matches(
                "[-+]?\\d+(\\.\\d+)?"
        );
    }

    private boolean isBoolean(String value) {
        return "true".equals(value)
                || "false".equals(value);
    }

    private String normalizeText(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}