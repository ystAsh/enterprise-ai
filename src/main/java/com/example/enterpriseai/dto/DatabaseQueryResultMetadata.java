/*
 * =============================================================================
 * 클래스명 : DatabaseQueryResultMetadata
 * =============================================================================
 * 목적
 *  - 검증 완료된 Database Query 결과의 공통 건수 정보를 표현한다.
 *  - 업무 데이터 구조와 사용자 표시 정책을 분리한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

public record DatabaseQueryResultMetadata(
        int returnedCount,
        Integer totalCount
) {

    public DatabaseQueryResultMetadata {

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
    }

    public static DatabaseQueryResultMetadata returned(
            int returnedCount
    ) {
        return new DatabaseQueryResultMetadata(
                returnedCount,
                null
        );
    }
}