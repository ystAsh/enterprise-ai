/*
 * =============================================================================
 * 클래스명 : DatabaseQueryParameterPolicy
 * =============================================================================
 * 목적
 *  - Database Query 파라미터의 공통 검증 규칙을 표현한다.
 *  - 파라미터 이름별 허용 타입, 필수 여부, 문자열 최대 길이를 서버에서 관리한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

public record DatabaseQueryParameterPolicy(

        // 허용할 파라미터 값 타입
        ParameterType type,

        // 필수 파라미터 여부
        boolean required,

        // 문자열 타입일 때 허용할 최대 길이
        Integer maxLength

) {

    public DatabaseQueryParameterPolicy {

        if (type == null) {
            throw new IllegalArgumentException(
                    "Query 파라미터 타입이 없습니다."
            );
        }

        if (type == ParameterType.STRING) {
            if (maxLength == null || maxLength <= 0) {
                throw new IllegalArgumentException(
                        "문자열 최대 길이는 1 이상이어야 합니다."
                );
            }
        } else if (maxLength != null) {
            throw new IllegalArgumentException(
                    "문자열이 아닌 타입에는 최대 길이를 지정할 수 없습니다."
            );
        }
    }

    public enum ParameterType {
        STRING,
        LONG
    }
}