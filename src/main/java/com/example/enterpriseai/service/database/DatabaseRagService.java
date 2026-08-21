/*
 * =============================================================================
 * 클래스명 : DatabaseRagService
 * =============================================================================
 * 목적
 *  - 검증 완료된 DatabaseQueryResult를 기반으로 Gemini 자연어 답변을 생성한다.
 *  - 업무별 Query 실행 로직과 AI 답변 생성 로직을 분리한다.
 *  - 특정 업무 Query나 업무별 문장을 하드코딩하지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class DatabaseRagService {

    private final ChatClient chatClient;

    public DatabaseRagService(
            ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient =
                chatClientBuilder.build();
    }

    /*
     * 사용자 질문과 검증 완료된 DB 결과를 이용해
     * Gemini 자연어 답변을 생성한다.
     *
     * Query 실행과 권한 검증은 이 Service에서 수행하지 않는다.
     */
    public String answer(
            String question,
            DatabaseQueryResult queryResult
    ) {

        validateInput(
                question,
                queryResult
        );

        // Gemini에는 검증 완료된 최소 업무 결과만 전달한다.
        return chatClient.prompt()
                .system("""
                        당신은 사내 업무 데이터 질의응답 AI입니다.

                        반드시 제공된 검증 완료 업무 데이터만 근거로 답변하세요.
                        제공되지 않은 내용을 추측하거나 만들어내지 마세요.
                        숫자나 업무 결과를 임의로 변경하지 마세요.

                        내부 시스템 구조, SQL, 권한정보를 추측하거나 설명하지 마세요.

                        사용자의 질문에 필요한 내용만
                        간결하고 자연스럽게 답변하세요.
                        """)
                .user("""
                        [사용자 질문]
                        %s

                        [업무 결과 유형]
                        %s

                        [검증 완료 업무 데이터]
                        %s
                        """.formatted(
                        question,
                        queryResult.queryType(),
                        queryResult.data()
                ))
                .call()
                .content();
    }

    /*
     * Gemini 호출 전에 질문과 검증 결과를 확인한다.
     */
    private void validateInput(
            String question,
            DatabaseQueryResult queryResult
    ) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }

        if (queryResult == null) {
            throw new IllegalArgumentException(
                    "검증된 DB 조회 결과가 없습니다."
            );
        }

        if (queryResult.queryType() == null
                || queryResult.queryType().isBlank()) {

            throw new IllegalArgumentException(
                    "DB 조회 결과 유형이 없습니다."
            );
        }

        if (queryResult.data() == null
                || queryResult.data().isEmpty()) {

            throw new IllegalArgumentException(
                    "DB 조회 결과 데이터가 없습니다."
            );
        }
    }
}