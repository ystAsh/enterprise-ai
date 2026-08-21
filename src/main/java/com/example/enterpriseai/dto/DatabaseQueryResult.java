/*
 * =============================================================================
 * 클래스명 : DatabaseQueryResult
 * =============================================================================
 * 목적
 *  - Database RAG에서 검증 완료된 업무 조회 결과를 공통 형태로 표현한다.
 *  - Gemini 답변용 데이터와 사용자에게 제공할 안전한 조회 근거를 분리한다.
 */

package com.example.enterpriseai.dto;

import java.util.Map;

public record DatabaseQueryResult(

        // 업무 Query 종류
        String queryType,

        // Gemini 답변 생성에 사용할 검증 완료 업무 데이터
        Map<String, Object> data,

        // 사용자에게 제공 가능한 안전한 조회 근거
        Evidence evidence

) {

    /*
     * 사용자에게 제공할 수 있는 안전한 조회 근거이다.
     * 내부 SQL이나 실제 권한 값은 포함하지 않는다.
     */
    public record Evidence(

            // 데이터 출처
            String source,

            // 서버 내부 Query 식별자
            String queryKey,

            // 사용자가 이해할 수 있는 조회 설명
            String queryName,

            // JPA / MyBatis / JdbcClient 등의 실행 방식
            String executionType,

            // Java 결과 검증 통과 여부
            boolean validated
    ) {
    }
}