/*
 * =============================================================================
 * 클래스명 : DatabaseQueryPlanValidator
 * =============================================================================
 * 목적
 *  - LLM이 생성한 DatabaseQueryPlanCandidate를 신뢰하지 않고 Java에서 검증한다.
 *  - 서버의 DatabaseQueryPlanValidationPolicy allowlist를 기준으로 허용 범위를 강제한다.
 *  - 검증 완료된 값만 DatabaseQueryPlan으로 변환한다.
 *  - 특정 회사, 업무, 테이블, 컬럼에 종속되지 않는다.
 */

package com.example.enterpriseai.service.security;

import com.example.enterpriseai.dto.DatabaseQueryPlan;
import com.example.enterpriseai.dto.DatabaseQueryPlanCandidate;
import com.example.enterpriseai.dto.DatabaseQueryPlanValidationPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DatabaseQueryPlanValidator {

    // LLM Candidate를 서버 정책으로 검증한 뒤 실행 가능한 Query Plan으로 변환한다.
    public DatabaseQueryPlan validate(
            DatabaseQueryPlanCandidate candidate,
            DatabaseQueryPlanValidationPolicy policy
    ) {
        if (candidate == null) {
            throw new IllegalArgumentException(
                    "Query Plan 후보가 없습니다."
            );
        }

        if (policy == null) {
            throw new IllegalArgumentException(
                    "Query Plan 검증 정책이 없습니다."
            );
        }

        List<String> selectFields = requireFields(
                candidate.selectFields(),
                "조회 필드"
        );

        Map<String, Object> filters = normalizeFilters(
                candidate.filters()
        );

        List<String> groupByFields = normalizeFields(
                candidate.groupByFields(),
                "그룹 필드"
        );

        List<String> orderByFields = normalizeFields(
                candidate.orderByFields(),
                "정렬 필드"
        );

        validateAllowedFields(
                selectFields,
                policy.allowedSelectFields(),
                "조회 필드"
        );

        validateAllowedFields(
                filters.keySet(),
                policy.allowedFilterFields(),
                "필터 필드"
        );

        validateAllowedFields(
                groupByFields,
                policy.allowedGroupByFields(),
                "그룹 필드"
        );

        validateAllowedFields(
                orderByFields,
                policy.allowedOrderByFields(),
                "정렬 필드"
        );

        validateFilterValues(filters);

        int maxRows = validateMaxRows(
                candidate.maxRows(),
                policy.maxRows()
        );

        return new DatabaseQueryPlan(
                selectFields,
                filters,
                groupByFields,
                orderByFields,
                maxRows
        );
    }

    private List<String> requireFields(
            List<String> fields,
            String fieldType
    ) {
        List<String> normalized = normalizeFields(
                fields,
                fieldType
        );

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldType + "가 없습니다."
            );
        }

        return normalized;
    }

    private List<String> normalizeFields(
            List<String> fields,
            String fieldType
    ) {
        if (fields == null) {
            return List.of();
        }

        return fields.stream()
                .map(field -> normalizeField(field, fieldType))
                .toList();
    }

    private String normalizeField(
            String field,
            String fieldType
    ) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(
                    fieldType + "에 빈 값이 포함되어 있습니다."
            );
        }

        return field.trim();
    }

    private Map<String, Object> normalizeFilters(
            Map<String, Object> filters
    ) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }

        return filters.entrySet().stream()
                .collect(
                        java.util.stream.Collectors.toUnmodifiableMap(
                                entry -> normalizeField(
                                        entry.getKey(),
                                        "필터 필드"
                                ),
                                Map.Entry::getValue
                        )
                );
    }

    private void validateAllowedFields(
            Iterable<String> requestedFields,
            Set<String> allowedFields,
            String fieldType
    ) {
        for (String requestedField : requestedFields) {
            if (!allowedFields.contains(requestedField)) {
                throw new IllegalArgumentException(
                        "허용되지 않은 " + fieldType + "입니다: "
                                + requestedField
                );
            }
        }
    }

    // 현재 단계에서는 단순 Binding 가능한 scalar 값만 허용한다.
    private void validateFilterValues(
            Map<String, Object> filters
    ) {
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            Object value = entry.getValue();

            if (value == null) {
                throw new IllegalArgumentException(
                        "필터 값이 없습니다: " + entry.getKey()
                );
            }

            if (!(value instanceof String)
                    && !(value instanceof Number)
                    && !(value instanceof Boolean)) {
                throw new IllegalArgumentException(
                        "허용되지 않은 필터 값 타입입니다: "
                                + entry.getKey()
                );
            }
        }
    }

    private int validateMaxRows(
            Integer requestedMaxRows,
            int policyMaxRows
    ) {
        if (requestedMaxRows == null) {
            return policyMaxRows;
        }

        if (requestedMaxRows <= 0) {
            throw new IllegalArgumentException(
                    "최대 조회 건수는 1 이상이어야 합니다."
            );
        }

        if (requestedMaxRows > policyMaxRows) {
            throw new IllegalArgumentException(
                    "서버에서 허용한 최대 조회 건수를 초과했습니다."
            );
        }

        return requestedMaxRows;
    }
}