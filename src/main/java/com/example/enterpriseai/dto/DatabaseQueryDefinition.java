/*
 * =============================================================================
 * 클래스명 : DatabaseQueryDefinition
 * =============================================================================
 * 목적
 *  - Database RAG에서 서버가 허용한 Query의 공통 정의를 표현한다.
 *  - Query 실행 전 보안 정책과 실행 후 결과 검증 정책을 함께 관리한다.
 *  - 특정 회사, 업무, 테이블 구조에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

public record DatabaseQueryDefinition(

        // Query 결과의 논리적 유형
        String queryType,

        // 서버 내부에서 사용하는 고유 Query 식별자
        String queryKey,

        // 사용자에게 제공 가능한 안전한 Query 설명
        String queryName,

        // 사용자에게 노출 가능한 논리적 데이터 출처명
        String source,

        // JPA_REPOSITORY / MYBATIS / JDBC_CLIENT 등의 실행 방식
        String executionType,

        // Query 실행 전에 적용할 서버 보안 정책
        DatabaseQueryExecutionPolicy executionPolicy,

        // Query 실행 결과에 적용할 공통 보안 검증 정책
        DatabaseValidationPolicy validationPolicy

) {

    public DatabaseQueryDefinition {

        if (queryType == null || queryType.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 유형이 없습니다."
            );
        }

        if (queryKey == null || queryKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 식별자가 없습니다."
            );
        }

        if (queryName == null || queryName.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 설명이 없습니다."
            );
        }

        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException(
                    "데이터 출처가 없습니다."
            );
        }

        if (executionType == null || executionType.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 실행 방식이 없습니다."
            );
        }

        if (executionPolicy == null) {
            throw new IllegalArgumentException(
                    "Query 실행 보안 정책이 없습니다."
            );
        }

        if (validationPolicy == null) {
            throw new IllegalArgumentException(
                    "Query 결과 검증 정책이 없습니다."
            );
        }
    }
}