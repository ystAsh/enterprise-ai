/*
 * =============================================================================
 * 클래스명 : DatabaseQueryPlan
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL에서 Java 검증을 통과한 Query Plan을 표현한다.
 *  - 검증되지 않은 DatabaseQueryPlanCandidate와 실행 가능한 계획을 분리한다.
 *  - SQL 문자열을 직접 생성하거나 보관하지 않는다.
 *  - 특정 회사, 업무, 테이블, 컬럼에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.List;
import java.util.Map;

public record DatabaseQueryPlan(
        List<String> selectFields,
        Map<String, Object> filters,
        List<String> groupByFields,
        List<String> orderByFields,
        int maxRows
) {

    public DatabaseQueryPlan {
        if (selectFields == null) {
            throw new IllegalArgumentException(
                    "조회 필드 정보가 없습니다."
            );
        }

        if (filters == null) {
            throw new IllegalArgumentException(
                    "조회 조건 정보가 없습니다."
            );
        }

        if (groupByFields == null) {
            throw new IllegalArgumentException(
                    "그룹 조건 정보가 없습니다."
            );
        }

        if (orderByFields == null) {
            throw new IllegalArgumentException(
                    "정렬 조건 정보가 없습니다."
            );
        }

        if (maxRows <= 0) {
            throw new IllegalArgumentException(
                    "최대 조회 건수는 1 이상이어야 합니다."
            );
        }

        selectFields = List.copyOf(selectFields);
        filters = Map.copyOf(filters);
        groupByFields = List.copyOf(groupByFields);
        orderByFields = List.copyOf(orderByFields);
    }
}