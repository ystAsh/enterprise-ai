/*
 * =============================================================================
 * 클래스명 : DatabaseQueryExecutionResult
 * =============================================================================
 * 목적
 *  - DatabaseQueryExecutor의 실행 결과와 공통 건수 정보를 표현한다.
 *  - 업무별 Executor가 알고 있는 반환 건수를 공통 실행 계층에 전달한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Map;

public record DatabaseQueryExecutionResult(
        Map<String, Object> data,
        int returnedCount,
        Integer totalCount
) {

    public DatabaseQueryExecutionResult {

        if (data == null) {
            throw new IllegalArgumentException(
                    "Query 실행 결과가 없습니다."
            );
        }

        if (returnedCount < 0) {
            throw new IllegalArgumentException(
                    "반환 건수는 0 이상이어야 합니다."
            );
        }

        if (totalCount != null && totalCount < 0) {
            throw new IllegalArgumentException(
                    "전체 건수는 0 이상이어야 합니다."
            );
        }

        if (totalCount != null
                && returnedCount > totalCount) {

            throw new IllegalArgumentException(
                    "반환 건수는 전체 건수를 초과할 수 없습니다."
            );
        }

        data = Map.copyOf(data);
    }

    public static DatabaseQueryExecutionResult returned(
            Map<String, Object> data,
            int returnedCount
    ) {

        return new DatabaseQueryExecutionResult(
                data,
                returnedCount,
                null
        );
    }
}