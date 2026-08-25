/*
 * =============================================================================
 * 클래스명 : DatabaseQueryParameters
 * =============================================================================
 * 목적
 *  - Database RAG에서 현재 질문에 필요한 실행 파라미터를 공통 형태로 전달한다.
 *  - SQL, 테이블명, Repository, 권한정보 등 서버 내부 실행 정보를 포함하지 않는다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record DatabaseQueryParameters(

        // Query 실행에 필요한 검증 완료 파라미터
        Map<String, Object> values

) {

    public DatabaseQueryParameters {

        if (values == null) {
            values = Map.of();
        } else {
            values = Collections.unmodifiableMap(
                    new HashMap<>(values)
            );
        }
    }

    /*
     * 필수 문자열 파라미터를 조회한다.
     */
    public String getRequiredString(
            String key
    ) {

        Object value = values.get(key);

        if (!(value instanceof String stringValue)
                || stringValue.isBlank()) {

            throw new IllegalArgumentException(
                    "필수 Query 파라미터가 없습니다."
            );
        }

        return stringValue.trim();
    }

    /*
     * 필수 Long 파라미터를 조회한다.
     */
    public Long getRequiredLong(
            String key
    ) {

        Object value = values.get(key);

        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }

        throw new IllegalArgumentException(
                "필수 Query 파라미터가 없습니다."
        );
    }

    /*
     * 파라미터 존재 여부를 확인한다.
     */
    public boolean contains(
            String key
    ) {
        return values.containsKey(key);
    }
}