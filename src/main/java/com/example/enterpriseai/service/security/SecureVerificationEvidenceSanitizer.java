/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidenceSanitizer
 * =============================================================================
 * 목적
 *  - Secure Verification Evidence에 저장하기 전 데이터를 서버 정책에 따라 정제한다.
 *  - 허용되지 않은 Parameter 저장과 민감 Parameter 원문 저장을 차단한다.
 *  - 대량 Result가 Evidence에 그대로 저장되지 않도록 저장 방식을 강제한다.
 *  - 실제 LLM Context와 최종 Answer가 정책 크기를 초과하는 경우 저장을 차단한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.security;

import com.example.enterpriseai.dto.SecureVerificationEvidencePolicy;
import org.springframework.stereotype.Component;

import java.time.temporal.TemporalAccessor;
import java.util.*;

@Component
public class SecureVerificationEvidenceSanitizer {

    private static final String MASKED_VALUE = "[MASKED]";

    // 정책에서 허용한 Parameter만 저장하고 민감 Parameter는 마스킹한다.
    public Map<String, Object> sanitizeParameters(
            Map<String, Object> parameters,
            SecureVerificationEvidencePolicy policy
    ) {
        requirePolicy(policy);

        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();

        for (String parameterName : policy.allowedParameterNames()) {
            if (!parameters.containsKey(parameterName)) {
                continue;
            }

            if (policy.maskedParameterNames().contains(parameterName)) {
                sanitized.put(parameterName, MASKED_VALUE);
                continue;
            }

            Object value = parameters.get(parameterName);

            if (!isAllowedParameterValue(value)) {
                throw new IllegalArgumentException(
                        "Secure Verification Evidence에 저장할 수 없는 Parameter 타입입니다: "
                                + parameterName
                );
            }

            sanitized.put(parameterName, value);
        }

        return Collections.unmodifiableMap(sanitized);
    }

    /*
     * SNAPSHOT:
     *  - 정책 범위 이하의 검증 완료 Result만 저장한다.
     *
     * REFERENCE_ONLY:
     *  - Raw Result는 저장하지 않고 resultReference/resultHash만 유지한다.
     */
    public SanitizedResult sanitizeResult(
            Map<String, Object> validatedResult,
            long resultCount,
            String resultReference,
            String resultHash,
            SecureVerificationEvidencePolicy policy
    ) {
        requirePolicy(policy);

        if (resultCount < 0) {
            throw new IllegalArgumentException(
                    "Result 건수가 올바르지 않습니다."
            );
        }

        return switch (policy.resultStorageMode()) {
            case SNAPSHOT -> sanitizeSnapshot(
                    validatedResult,
                    resultCount,
                    resultHash,
                    policy
            );

            case REFERENCE_ONLY -> sanitizeReferenceOnly(
                    resultReference,
                    resultHash
            );
        };
    }

    // 실제 Gemini에 전달한 Context 원문을 길이 제한 안에서만 허용한다.
    public String sanitizeContext(
            String llmContext,
            SecureVerificationEvidencePolicy policy
    ) {
        requirePolicy(policy);

        return requireTextWithinLimit(
                llmContext,
                policy.maxContextLength(),
                "LLM Context"
        );
    }

    // 실제 최종 Answer 원문을 길이 제한 안에서만 허용한다.
    public String sanitizeAnswer(
            String finalAnswer,
            SecureVerificationEvidencePolicy policy
    ) {
        requirePolicy(policy);

        return requireTextWithinLimit(
                finalAnswer,
                policy.maxAnswerLength(),
                "최종 AI 답변"
        );
    }

    private SanitizedResult sanitizeSnapshot(
            Map<String, Object> validatedResult,
            long resultCount,
            String resultHash,
            SecureVerificationEvidencePolicy policy
    ) {
        if (resultCount > policy.maxStoredResultRows()) {
            throw new IllegalArgumentException(
                    "Secure Verification Evidence Snapshot 최대 행 수를 초과했습니다."
            );
        }

        if (validatedResult == null) {
            throw new IllegalArgumentException(
                    "검증 완료 Result가 없습니다."
            );
        }

        return new SanitizedResult(
                immutableMap(validatedResult),
                null,
                normalizeOptionalText(resultHash)
        );
    }

    private SanitizedResult sanitizeReferenceOnly(
            String resultReference,
            String resultHash
    ) {
        String normalizedReference =
                normalizeOptionalText(resultReference);

        String normalizedHash =
                normalizeOptionalText(resultHash);

        if (normalizedReference == null && normalizedHash == null) {
            throw new IllegalArgumentException(
                    "REFERENCE_ONLY Result에는 resultReference 또는 resultHash가 필요합니다."
            );
        }

        return new SanitizedResult(
                Map.of(),
                normalizedReference,
                normalizedHash
        );
    }

    private boolean isAllowedParameterValue(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof TemporalAccessor;
    }

    private String requireTextWithinLimit(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "는 필수입니다."
            );
        }

        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "가 저장 허용 길이를 초과했습니다."
            );
        }

        return value;
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank()
                ? null
                : value;
    }

    private void requirePolicy(
            SecureVerificationEvidencePolicy policy
    ) {
        if (policy == null) {
            throw new IllegalArgumentException(
                    "Secure Verification Evidence Policy는 필수입니다."
            );
        }
    }

    private Map<String, Object> immutableMap(
            Map<String, Object> source
    ) {
        Map<String, Object> copied = new LinkedHashMap<>();

        source.forEach(
                (key, value) ->
                        copied.put(
                                key,
                                immutableValue(value)
                        )
        );

        return Collections.unmodifiableMap(copied);
    }

    private Object immutableValue(Object value) {

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copied = new LinkedHashMap<>();

            map.forEach((key, nestedValue) -> {
                if (!(key instanceof String stringKey)) {
                    throw new IllegalArgumentException(
                            "Result Map Key는 문자열이어야 합니다."
                    );
                }

                copied.put(
                        stringKey,
                        immutableValue(nestedValue)
                );
            });

            return Collections.unmodifiableMap(copied);
        }

        if (value instanceof List<?> list) {
            List<Object> copied = new ArrayList<>();

            for (Object item : list) {
                copied.add(
                        immutableValue(item)
                );
            }

            return Collections.unmodifiableList(copied);
        }

        return value;
    }

    public record SanitizedResult(
            Map<String, Object> storedResult,
            String resultReference,
            String resultHash
    ) {

        public SanitizedResult {
            storedResult = storedResult == null
                    ? Map.of()
                    : storedResult;
        }
    }
}