/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidence
 * =============================================================================
 * 목적
 *  - 개발자/관리자가 AI 답변의 실제 근거를 재검증하기 위한 내부 Evidence를 표현한다.
 *  - 일반 사용자 Evidence 및 일반 Audit와 분리하여 관리한다.
 *  - 기존 시스템이 제공한 실제 Query 정보 또는 Query 식별정보를 그대로 연결한다.
 *  - 기존 시스템이 Query Evidence를 제공하지 않는 경우에도 그 상태를 그대로 표현한다.
 *  - 실제 LLM 호출이 없는 경로는 llmContext가 존재하지 않는 상태로 표현한다.
 *  - Answer Verification 전에는 verificationStatus가 없는 상태를 허용한다.
 *  - 저장 정책을 통과한 Parameter와 검증 완료 Result만 포함한다.
 *  - 대량 Result는 전체 Rows 대신 resultReference/resultHash로 연결한다.
 *  - 일반 API 응답에 직접 노출하지 않는다.
 */

package com.example.enterpriseai.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record SecureVerificationEvidence(
        String question,
        String source,
        String queryKey,
        String executionType,
        QueryEvidence queryEvidence,
        Map<String, Object> storedParameters,
        Map<String, Object> validatedResult,
        long resultCount,
        String resultReference,
        String resultHash,
        String llmContext,
        String finalAnswer,
        VerificationStatus verificationStatus,
        LocalDateTime executedAt
) {

    public SecureVerificationEvidence {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "검증 대상 질문은 필수입니다."
            );
        }

        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException(
                    "검증 대상 Source는 필수입니다."
            );
        }

        if (queryKey == null || queryKey.isBlank()) {
            throw new IllegalArgumentException(
                    "검증 대상 Query Key는 필수입니다."
            );
        }

        if (executionType == null || executionType.isBlank()) {
            throw new IllegalArgumentException(
                    "검증 대상 실행 방식은 필수입니다."
            );
        }

        if (queryEvidence == null) {
            throw new IllegalArgumentException(
                    "Query Evidence 상태 정보는 필수입니다."
            );
        }

        if (resultCount < 0) {
            throw new IllegalArgumentException(
                    "검증 대상 결과 건수가 올바르지 않습니다."
            );
        }

        storedParameters = storedParameters == null
                ? Map.of()
                : Map.copyOf(storedParameters);

        validatedResult = validatedResult == null
                ? Map.of()
                : Map.copyOf(validatedResult);

        resultReference = normalizeOptionalText(resultReference);
        resultHash = normalizeOptionalText(resultHash);
        llmContext = normalizeOptionalText(llmContext);

        // Raw Result가 없으면 참조정보 또는 Hash가 있어야 재검증할 수 있다.
        if (validatedResult.isEmpty()
                && resultReference == null
                && resultHash == null) {

            throw new IllegalArgumentException(
                    "Result Snapshot이 없으면 resultReference 또는 resultHash가 필요합니다."
            );
        }

        if (finalAnswer == null || finalAnswer.isBlank()) {
            throw new IllegalArgumentException(
                    "최종 답변은 필수입니다."
            );
        }

        if (executedAt == null) {
            throw new IllegalArgumentException(
                    "실행 시각은 필수입니다."
            );
        }
    }

    /*
     * 실제 Query의 원본 소유자가 제공한 검증 정보를 표현한다.
     *
     * 실제 Query 또는 Query Reference를 제공하는 시스템:
     *  - 제공된 값을 그대로 연결한다.
     *
     * 제공하지 않는 시스템:
     *  - 모든 필드를 null로 유지한다.
     *  - enterprise-ai가 Query Evidence를 추측하거나 임의 생성하지 않는다.
     */
    public record QueryEvidence(
            String actualQuery,
            String queryReference,
            String queryVersion,
            String queryHash
    ) {

        public QueryEvidence {
            actualQuery = normalizeOptionalText(actualQuery);
            queryReference = normalizeOptionalText(queryReference);
            queryVersion = normalizeOptionalText(queryVersion);
            queryHash = normalizeOptionalText(queryHash);
        }
    }

    /*
     * null:
     *  - Answer Verification 수행 전
     *
     * 아래 값:
     *  - 실제 Answer Verification 수행 결과
     */
    public enum VerificationStatus {
        MATCH,
        MISMATCH,
        UNSUPPORTED,
        MISSING
    }

    private static String normalizeOptionalText(
            String value
    ) {
        return value == null || value.isBlank()
                ? null
                : value;
    }
}