/*
 * =============================================================================
 * 클래스명 : DatabaseQueryResult
 * =============================================================================
 * 목적
 *  - 검증 완료된 Database Query 결과와 안전한 조회 근거를 표현한다.
 *  - 검증 완료 데이터와 공통 결과 건수 Metadata를 함께 전달한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Map;

public record DatabaseQueryResult(
        String queryType,
        Map<String, Object> data,
        DatabaseQueryResultMetadata metadata,
        Evidence evidence
) {

    public DatabaseQueryResult {

        if (queryType == null || queryType.isBlank()) {
            throw new IllegalArgumentException(
                    "Query Type은 필수입니다."
            );
        }

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException(
                    "검증 완료된 Query 결과가 없습니다."
            );
        }

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Query 결과 Metadata는 필수입니다."
            );
        }

        if (evidence == null) {
            throw new IllegalArgumentException(
                    "Query Evidence는 필수입니다."
            );
        }

        if (!evidence.validated()) {
            throw new IllegalArgumentException(
                    "검증되지 않은 Query 결과는 사용할 수 없습니다."
            );
        }

        data = Map.copyOf(data);
    }

    public record Evidence(
            String source,
            String queryKey,
            String queryName,
            String executionType,
            boolean validated
    ) {

        public Evidence {

            if (source == null || source.isBlank()) {
                throw new IllegalArgumentException(
                        "Evidence Source는 필수입니다."
                );
            }

            if (queryKey == null || queryKey.isBlank()) {
                throw new IllegalArgumentException(
                        "Evidence Query Key는 필수입니다."
                );
            }

            if (queryName == null || queryName.isBlank()) {
                throw new IllegalArgumentException(
                        "Evidence Query Name은 필수입니다."
                );
            }

            if (executionType == null || executionType.isBlank()) {
                throw new IllegalArgumentException(
                        "Evidence Execution Type은 필수입니다."
                );
            }
        }
    }
}