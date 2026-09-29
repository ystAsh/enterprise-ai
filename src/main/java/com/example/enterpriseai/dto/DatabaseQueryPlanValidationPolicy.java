/*
 * =============================================================================
 * 클래스명 : DatabaseQueryPlanValidationPolicy
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL Query Plan을 Java에서 검증하기 위한 서버 정책을 표현한다.
 *  - 조회/필터/그룹/정렬에 사용할 수 있는 필드를 allowlist 방식으로 제한한다.
 *  - 최대 조회 건수를 서버 정책으로 강제한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Set;

public record DatabaseQueryPlanValidationPolicy(
        Set<String> allowedSelectFields,
        Set<String> allowedFilterFields,
        Set<String> allowedGroupByFields,
        Set<String> allowedOrderByFields,
        int maxRows
) {

    public DatabaseQueryPlanValidationPolicy {
        if (allowedSelectFields == null || allowedSelectFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "허용 조회 필드 정책이 없습니다."
            );
        }

        if (allowedFilterFields == null) {
            throw new IllegalArgumentException(
                    "허용 필터 필드 정책이 없습니다."
            );
        }

        if (allowedGroupByFields == null) {
            throw new IllegalArgumentException(
                    "허용 그룹 필드 정책이 없습니다."
            );
        }

        if (allowedOrderByFields == null) {
            throw new IllegalArgumentException(
                    "허용 정렬 필드 정책이 없습니다."
            );
        }

        if (maxRows <= 0) {
            throw new IllegalArgumentException(
                    "최대 조회 건수 정책은 1 이상이어야 합니다."
            );
        }

        allowedSelectFields = Set.copyOf(allowedSelectFields);
        allowedFilterFields = Set.copyOf(allowedFilterFields);
        allowedGroupByFields = Set.copyOf(allowedGroupByFields);
        allowedOrderByFields = Set.copyOf(allowedOrderByFields);
    }
}