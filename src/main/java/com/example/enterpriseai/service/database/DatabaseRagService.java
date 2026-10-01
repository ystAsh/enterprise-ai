/*
 * =============================================================================
 * 클래스명 : DatabaseRagService
 * =============================================================================
 * 목적
 *  - 검증 완료된 DatabaseQueryResult를 기반으로 Gemini 자연어 답변을 생성한다.
 *  - Query 실행/권한 검증/업무 의미 판단과 AI 답변 생성을 분리한다.
 *  - 특정 업무 도메인에 종속되지 않는 공통 Database RAG 답변 계층이다.
 *  - 실제 Gemini에 전달한 검증 완료 Context를 최종 답변과 함께 반환할 수 있다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class DatabaseRagService {

    private static final String SYSTEM_PROMPT = """
            당신은 검증 완료된 데이터만 설명하는 AI입니다.

            반드시 제공된 검증 완료 데이터만 근거로 답변하세요.
            제공되지 않은 내용을 추측하거나 만들어내지 마세요.
            숫자나 결과 값을 임의로 변경하지 마세요.

            내부 시스템 구조, SQL, 인증정보, 권한정보를
            추측하거나 설명하지 마세요.

            사용자의 질문에 필요한 내용만
            간결하고 자연스럽게 답변하세요.
            """;

    private final ChatClient chatClient;

    public DatabaseRagService(
            ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient =
                chatClientBuilder.build();
    }

    /*
     * 기존 호출부 호환용이다.
     * Secure Verification Evidence가 필요한 호출부는 answerWithContext()를 사용한다.
     */
    public String answer(
            String question,
            DatabaseQueryResult queryResult
    ) {
        return answerWithContext(
                question,
                queryResult
        ).answer();
    }

    /*
     * 검증 완료된 DB 결과만 Gemini에 전달하고,
     * 실제 전달한 Context와 최종 답변을 함께 반환한다.
     */
    public DatabaseRagAnswer answerWithContext(
            String question,
            DatabaseQueryResult queryResult
    ) {
        validateInput(
                question,
                queryResult
        );

        String llmContext =
                """
                        [사용자 질문]
                        %s

                        [결과 유형]
                        %s

                        [검증 완료 데이터]
                        %s
                        """.formatted(
                        question,
                        queryResult.queryType(),
                        queryResult.data()
                );

        String answer =
                chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(llmContext)
                        .call()
                        .content();

        return new DatabaseRagAnswer(
                answer,
                llmContext
        );
    }

    /*
     * Gemini 호출 전에 입력값이 최소 조건을 만족하는지 확인한다.
     *
     * 실제 결과 보안 검증은 DatabaseResultValidator에서 수행한다.
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

    /*
     * 실제 Gemini에 전달한 Context와 생성된 최종 답변을 함께 유지한다.
     */
    public record DatabaseRagAnswer(
            String answer,
            String llmContext
    ) {
        public DatabaseRagAnswer {
            if (answer == null || answer.isBlank()) {
                throw new IllegalArgumentException(
                        "Gemini 최종 답변이 없습니다."
                );
            }

            if (llmContext == null || llmContext.isBlank()) {
                throw new IllegalArgumentException(
                        "Gemini 전달 Context가 없습니다."
                );
            }
        }
    }
}