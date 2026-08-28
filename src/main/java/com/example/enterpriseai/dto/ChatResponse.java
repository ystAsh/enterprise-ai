/*
 * =============================================================================
 * 클래스명 : ChatResponse
 * =============================================================================
 * 목적
 *  - AI가 생성한 자연어 답변과 사용자 화면에 필요한 안전한 결과 정보를 반환한다.
 *  - Database RAG의 내부 Query 실행정보와 사용자 표시정보를 분리한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Map;

public record ChatResponse(

        // Gemini가 생성한 자연어 답변
        String answer,

        // 화면에 구조화하여 표시할 수 있는 검증 완료 최소 데이터
        Map<String, Object> data,

        // 조회 조건에 해당하는 전체 결과 건수
        Integer totalCount,

        // 현재 응답에 포함된 결과 건수
        Integer returnedCount,

        // 전체 결과가 현재 응답보다 더 존재하는지 여부
        boolean hasMore,

        // 대량 결과 전체보기에 사용할 서버 발급 안전한 참조값
        String resultReference,

        // 다운로드 기능 제공 여부
        boolean downloadAvailable

) {

    /*
     * 기존 일반 채팅 응답과의 호환성을 유지한다.
     *
     * Document RAG 등 결과 메타정보가 필요하지 않은 응답은
     * answer만 반환할 수 있다.
     */
    public ChatResponse(
            String answer
    ) {
        this(
                answer,
                null,
                null,
                null,
                false,
                null,
                false
        );
    }

    public ChatResponse {

        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException(
                    "AI 답변이 없습니다."
            );
        }

        if (data != null) {
            data = Map.copyOf(data);
        }

        if (totalCount != null && totalCount < 0) {
            throw new IllegalArgumentException(
                    "전체 결과 건수는 0 이상이어야 합니다."
            );
        }

        if (returnedCount != null && returnedCount < 0) {
            throw new IllegalArgumentException(
                    "반환 결과 건수는 0 이상이어야 합니다."
            );
        }

        if (totalCount != null
                && returnedCount != null
                && returnedCount > totalCount) {

            throw new IllegalArgumentException(
                    "반환 결과 건수가 전체 결과 건수보다 클 수 없습니다."
            );
        }
    }
}