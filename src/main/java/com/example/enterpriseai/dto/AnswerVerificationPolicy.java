/*
 * =============================================================================
 * 클래스명 : AnswerVerificationPolicy
 * =============================================================================
 * 목적
 *  - Answer Verification에서 반드시 답변에 포함되어야 하는 Fact를 정의한다.
 *  - 모든 조회 결과 Field를 무조건 MISSING 검증 대상으로 취급하지 않도록 한다.
 *  - 특정 회사, 업무, 필드명을 공통 검증 계층에 하드코딩하지 않는다.
 *  - 업무별 필수 Fact 선택은 서버 정책/Adapter/Config 영역에서 결정할 수 있게 한다.
 */

package com.example.enterpriseai.dto;

import java.util.Set;

public record AnswerVerificationPolicy(
        Set<String> requiredFactReferences
) {

    public AnswerVerificationPolicy {
        requiredFactReferences = requiredFactReferences == null
                ? Set.of()
                : Set.copyOf(requiredFactReferences);

        for (String factReference : requiredFactReferences) {
            if (factReference == null || factReference.isBlank()) {
                throw new IllegalArgumentException(
                        "필수 Answer Verification Fact 참조값이 올바르지 않습니다."
                );
            }
        }
    }

    // 해당 Fact가 답변에 반드시 포함되어야 하는지 확인한다.
    public boolean isRequired(String factReference) {
        if (factReference == null || factReference.isBlank()) {
            return false;
        }

        return requiredFactReferences.contains(
                factReference.trim()
        );
    }

    public static AnswerVerificationPolicy noneRequired() {
        return new AnswerVerificationPolicy(
                Set.of()
        );
    }
}