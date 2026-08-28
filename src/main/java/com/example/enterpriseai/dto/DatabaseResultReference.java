/*
 * =============================================================================
 * 클래스명 : DatabaseResultReference
 * =============================================================================
 * 목적
 *  - 대량 Database Query 결과를 다시 조회하기 위한 서버 내부 참조 정보를 표현한다.
 *  - 브라우저에 내부 Query 정보나 사용자 권한 정보를 직접 노출하지 않는다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.time.Instant;
import java.util.Map;

public record DatabaseResultReference(
        String referenceId,
        Map<String, Object> data,
        DatabaseQueryResultMetadata metadata,
        Instant createdAt
) {

    public DatabaseResultReference {

        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException(
                    "결과 참조 ID는 필수입니다."
            );
        }

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException(
                    "결과 데이터는 필수입니다."
            );
        }

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "결과 Metadata는 필수입니다."
            );
        }

        if (createdAt == null) {
            throw new IllegalArgumentException(
                    "결과 생성 시각은 필수입니다."
            );
        }

        data = Map.copyOf(data);
    }
}