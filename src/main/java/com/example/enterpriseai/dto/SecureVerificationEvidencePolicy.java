/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidencePolicy
 * =============================================================================
 * 목적
 *  - Secure Verification Evidence에 저장할 수 있는 범위를 서버 정책으로 제한한다.
 *  - 실제 Query, Parameter, Result의 무제한 저장을 방지한다.
 *  - 민감 Parameter와 대량 Result가 검증 Evidence에 그대로 저장되지 않도록 한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Set;

public record SecureVerificationEvidencePolicy(
        boolean allowActualQuery,
        Set<String> allowedParameterNames,
        Set<String> maskedParameterNames,
        ResultStorageMode resultStorageMode,
        int maxStoredResultRows,
        int maxContextLength,
        int maxAnswerLength,
        int retentionDays
) {

    public SecureVerificationEvidencePolicy {

        allowedParameterNames = allowedParameterNames == null
                ? Set.of()
                : Set.copyOf(allowedParameterNames);

        maskedParameterNames = maskedParameterNames == null
                ? Set.of()
                : Set.copyOf(maskedParameterNames);

        if (!allowedParameterNames.containsAll(maskedParameterNames)) {
            throw new IllegalArgumentException(
                    "마스킹 Parameter는 저장 허용 Parameter에 포함되어야 합니다."
            );
        }

        if (resultStorageMode == null) {
            throw new IllegalArgumentException(
                    "Result 저장 방식은 필수입니다."
            );
        }

        if (maxStoredResultRows <= 0) {
            throw new IllegalArgumentException(
                    "저장 가능한 Result 최대 행 수는 1 이상이어야 합니다."
            );
        }

        if (maxContextLength <= 0) {
            throw new IllegalArgumentException(
                    "LLM Context 최대 길이는 1 이상이어야 합니다."
            );
        }

        if (maxAnswerLength <= 0) {
            throw new IllegalArgumentException(
                    "최종 답변 최대 길이는 1 이상이어야 합니다."
            );
        }

        if (retentionDays <= 0) {
            throw new IllegalArgumentException(
                    "Evidence 보관 기간은 1일 이상이어야 합니다."
            );
        }
    }

    public enum ResultStorageMode {

        /*
         * 검증 완료 Result가 충분히 작을 때만 제한된 Snapshot을 저장한다.
         */
        SNAPSHOT,

        /*
         * 대량 결과는 전체 Rows를 저장하지 않고
         * 별도 결과 참조정보와 Hash/건수로 연결한다.
         */
        REFERENCE_ONLY
    }
}