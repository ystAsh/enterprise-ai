/*
 * =============================================================================
 * 클래스명 : DatabaseValidationPolicy
 * =============================================================================
 * 목적
 *  - Database RAG의 조회 결과에 적용할 공통 보안/검증 정책을 정의한다.
 *  - 특정 업무, 회사, 테이블에 종속되지 않는다.
 *  - Query별 허용 범위만 외부에서 주입받아 공통 Validator가 사용하도록 한다.
 */

package com.example.enterpriseai.dto;

import java.util.Set;

public record DatabaseValidationPolicy(

        // LLM 전달을 허용할 결과 필드
        Set<String> allowedFields,

        // LLM 전달을 금지할 민감 필드
        Set<String> sensitiveFields,

        // 결과의 최대 필드 수
        int maxFields,

        // List 등의 최대 결과 건수
        int maxRows,

        // 문자열 하나의 최대 길이
        int maxStringLength,

        // Map/List 등의 최대 중첩 깊이
        int maxDepth,

        // NULL 값 허용 여부
        boolean allowNullValues,

        // 빈 결과 허용 여부
        boolean allowEmpty

) {

    public DatabaseValidationPolicy {

        allowedFields =
                allowedFields == null
                        ? Set.of()
                        : Set.copyOf(allowedFields);

        sensitiveFields =
                sensitiveFields == null
                        ? Set.of()
                        : Set.copyOf(sensitiveFields);

        if (maxFields <= 0) {
            throw new IllegalArgumentException(
                    "maxFields는 1 이상이어야 합니다."
            );
        }

        if (maxRows <= 0) {
            throw new IllegalArgumentException(
                    "maxRows는 1 이상이어야 합니다."
            );
        }

        if (maxStringLength <= 0) {
            throw new IllegalArgumentException(
                    "maxStringLength는 1 이상이어야 합니다."
            );
        }

        if (maxDepth < 0) {
            throw new IllegalArgumentException(
                    "maxDepth는 0 이상이어야 합니다."
            );
        }

        // 민감 필드가 허용 필드에도 동시에 등록되는 잘못된 정책을 차단한다.
        boolean conflict =
                sensitiveFields.stream()
                        .anyMatch(allowedFields::contains);

        if (conflict) {
            throw new IllegalArgumentException(
                    "허용 필드와 민감 필드 정책이 충돌합니다."
            );
        }
    }
}