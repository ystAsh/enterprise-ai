/*
 * =============================================================================
 * 클래스명 : DatabaseQueryExecutionPolicy
 * =============================================================================
 * 목적
 *  - Database RAG Query 실행 전에 적용할 공통 보안 정책을 정의한다.
 *  - 특정 회사, 업무, 테이블에 종속되지 않는다.
 *  - Query 실행 허용 범위와 실행 제한을 공통 계약으로 제공한다.
 */

package com.example.enterpriseai.dto;

import java.util.Set;

public record DatabaseQueryExecutionPolicy(

        // 실행을 허용할 Query 식별자
        Set<String> allowedQueryKeys,

        // 허용할 Query 실행 방식
        Set<String> allowedExecutionTypes,

        // 최대 조회 건수
        int maxRows,

        // Query Timeout
        long timeoutMs,

        // Database RAG Query의 ReadOnly 강제 여부
        boolean readOnly

) {

    public DatabaseQueryExecutionPolicy {

        allowedQueryKeys =
                allowedQueryKeys == null
                        ? Set.of()
                        : Set.copyOf(allowedQueryKeys);

        allowedExecutionTypes =
                allowedExecutionTypes == null
                        ? Set.of()
                        : Set.copyOf(allowedExecutionTypes);

        if (maxRows <= 0) {
            throw new IllegalArgumentException(
                    "maxRows는 1 이상이어야 합니다."
            );
        }

        if (timeoutMs <= 0) {
            throw new IllegalArgumentException(
                    "timeoutMs는 1 이상이어야 합니다."
            );
        }

        if (!readOnly) {
            throw new IllegalArgumentException(
                    "Database RAG Query는 ReadOnly여야 합니다."
            );
        }
    }
}