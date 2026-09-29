/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQuery
 * =============================================================================
 * 목적
 *  - 검증 완료 Query Plan과 서버 정책으로 생성된 Safe Query를 표현한다.
 *  - SQL 문자열과 Binding Parameter를 하나의 불변 객체로 관리한다.
 *  - LLM이 직접 생성한 SQL을 표현하는 용도로 사용하지 않는다.
 *  - 실제 실행 전용 내부 객체이며 외부 API나 LLM에 노출하지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Map;

public record DatabaseSafeQuery(
        String sql,
        Map<String, Object> parameters,
        int maxRows
) {

    public DatabaseSafeQuery {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(
                    "실행할 Safe Query가 없습니다."
            );
        }

        if (parameters == null) {
            throw new IllegalArgumentException(
                    "Query Binding 정보가 없습니다."
            );
        }

        if (maxRows <= 0) {
            throw new IllegalArgumentException(
                    "최대 조회 건수는 1 이상이어야 합니다."
            );
        }

        sql = sql.trim();
        parameters = Map.copyOf(parameters);
    }
}