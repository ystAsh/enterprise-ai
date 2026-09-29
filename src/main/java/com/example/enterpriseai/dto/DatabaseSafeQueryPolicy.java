/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQueryPolicy
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL 실행에 필요한 서버 내부 정책을 표현한다.
 *  - LLM에 물리 DB 구조를 노출하지 않고 실제 조회 대상과 필드 매핑을 서버가 결정한다.
 *  - Safe Query 실행 범위와 결과 검증 정책, Evidence 정보를 서버에서 관리한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Map;
import java.util.Set;

public record DatabaseSafeQueryPolicy(
        String queryType,
        String queryKey,
        String queryName,
        String source,
        String executionType,
        String tableName,
        Map<String, String> fieldMappings,
        Set<String> allowedOrderByFields,
        int maxRows,
        DatabaseValidationPolicy validationPolicy
) {

    public DatabaseSafeQueryPolicy {
        queryType = requireText(queryType, "Query Type");
        queryKey = requireText(queryKey, "Query Key");
        queryName = requireText(queryName, "Query Name");
        source = requireText(source, "Database Source");
        executionType = requireText(executionType, "Execution Type");
        tableName = requireText(tableName, "조회 대상");

        if (fieldMappings == null || fieldMappings.isEmpty()) {
            throw new IllegalArgumentException(
                    "필드 매핑 정책이 없습니다."
            );
        }

        if (allowedOrderByFields == null) {
            throw new IllegalArgumentException(
                    "허용 정렬 필드 정책이 없습니다."
            );
        }

        if (maxRows <= 0) {
            throw new IllegalArgumentException(
                    "최대 조회 건수는 1 이상이어야 합니다."
            );
        }

        if (validationPolicy == null) {
            throw new IllegalArgumentException(
                    "Safe Query 결과 검증 정책이 없습니다."
            );
        }

        if (validationPolicy.maxRows() > maxRows) {
            throw new IllegalArgumentException(
                    "결과 검증 최대 건수는 Safe Query 최대 조회 건수를 초과할 수 없습니다."
            );
        }

        fieldMappings = Map.copyOf(fieldMappings);
        allowedOrderByFields = Set.copyOf(allowedOrderByFields);
    }

    private static String requireText(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " 정보가 없습니다."
            );
        }

        return value.trim();
    }
}