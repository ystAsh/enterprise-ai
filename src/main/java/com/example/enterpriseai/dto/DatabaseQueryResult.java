/*
 * =============================================================================
 * 클래스명 : DatabaseQueryResult
 * =============================================================================
 * 목적
 *  - Database RAG에서 검증 완료된 조회 결과를 공통 형태로 표현한다.
 *  - LLM에 전달할 데이터와 사용자에게 제공할 안전한 조회 근거를 분리한다.
 *  - 특정 시스템, 업무, 테이블 구조에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Map;

public record DatabaseQueryResult(

        // 실행된 Query의 논리적 유형
        String queryType,

        // LLM에 전달할 검증 완료 최소 데이터
        Map<String, Object> data,

        // 사용자에게 제공 가능한 안전한 조회 근거
        Evidence evidence

) {

    /*
     * 검증 완료 결과만 생성할 수 있도록 기본 상태를 확인한다.
     */
    public DatabaseQueryResult {

        if (queryType == null || queryType.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 유형이 없습니다."
            );
        }

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException(
                    "검증 완료 조회 데이터가 없습니다."
            );
        }

        if (evidence == null) {
            throw new IllegalArgumentException(
                    "조회 근거가 없습니다."
            );
        }

        if (!evidence.validated()) {
            throw new IllegalArgumentException(
                    "검증되지 않은 결과는 DatabaseQueryResult로 생성할 수 없습니다."
            );
        }

        // 검증 이후 외부 코드가 최상위 Map을 변경하지 못하도록 복사한다.
        data = Map.copyOf(data);
    }

    /*
     * 사용자에게 공개 가능한 안전한 조회 근거이다.
     *
     * SQL, 사용자 식별자, 권한 범위, 내부 구현 세부정보는 포함하지 않는다.
     */
    public record Evidence(

            // 데이터 출처의 논리적 이름
            String source,

            // 서버 내부에서 사용하는 Query 식별자
            String queryKey,

            // 사용자가 이해할 수 있는 조회 설명
            String queryName,

            // JPA / MyBatis / JdbcClient 등의 실행 방식
            String executionType,

            // 서버 검증 통과 여부
            boolean validated

    ) {

        public Evidence {

            if (source == null || source.isBlank()) {
                throw new IllegalArgumentException(
                        "데이터 출처가 없습니다."
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

            if (executionType == null || executionType.isBlank()) {
                throw new IllegalArgumentException(
                        "Query 실행 방식이 없습니다."
                );
            }
        }
    }
}