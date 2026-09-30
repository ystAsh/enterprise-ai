/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidence
 * =============================================================================
 * 목적
 *  - 개발자/관리자가 AI 답변의 실제 근거를 재검증하기 위한 내부 Evidence를 표현한다.
 *  - 일반 사용자 Evidence 및 일반 Audit와 분리하여 관리한다.
 *  - 기존 시스템이 제공한 실제 Query 정보 또는 Query 식별정보를 그대로 연결한다.
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
                    "실제 Query Evidence는 필수입니다."
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

        // Raw Result가 없으면 참조정보 또는 Hash가 있어야 재검증할 수 있다.
        if (validatedResult.isEmpty()
                && resultReference == null
                && resultHash == null) {

            throw new IllegalArgumentException(
                    "Result Snapshot이 없으면 resultReference 또는 resultHash가 필요합니다."
            );
        }

        if (llmContext == null || llmContext.isBlank()) {
            throw new IllegalArgumentException(
                    "실제 LLM 전달 Context는 필수입니다."
            );
        }

        if (finalAnswer == null || finalAnswer.isBlank()) {
            throw new IllegalArgumentException(
                    "최종 AI 답변은 필수입니다."
            );
        }

        if (verificationStatus == null) {
            throw new IllegalArgumentException(
                    "답변 검증 상태는 필수입니다."
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
     * actualQuery:
     *  - 실제 Query를 안전하게 제공할 수 있을 때만 사용한다.
     *
     * queryReference:
     *  - 외부 시스템이 실제 Query를 직접 제공하지 않는 경우
     *    실행 로그 ID 등의 식별정보를 사용한다.
     *
     * 둘 중 최소 하나는 존재해야 한다.
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

            if (actualQuery == null && queryReference == null) {
                throw new IllegalArgumentException(
                        "실제 Query 또는 Query 식별정보가 필요합니다."
                );
            }
        }
    }

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