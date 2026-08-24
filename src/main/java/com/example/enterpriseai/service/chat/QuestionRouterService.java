/*
 * =============================================================================
 * 클래스명 : QuestionRouterService
 * =============================================================================
 * 목적
 *  - 사용자의 자연어 질문을 DOCUMENT / DATABASE / HYBRID 유형으로 분류한다.
 *  - Router는 질문의 처리 경로만 결정하며 권한이나 DB 데이터를 판단하지 않는다.
 *  - 사용자 질문 외의 내부 권한정보, SQL, Schema는 Gemini에 전달하지 않는다.
 */

package com.example.enterpriseai.service.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class QuestionRouterService {

    private final ChatClient chatClient;

    public QuestionRouterService(
            ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient =
                chatClientBuilder.build();
    }

    /*
     * 사용자 질문이 어떤 RAG 경로를 사용해야 하는지 분류한다.
     */
    public QuestionType route(
            String question
    ) {

        validateQuestion(question);

        String result =
                chatClient.prompt()
                        .system("""
                                사용자의 질문을 아래 세 가지 중 하나로만 분류하세요.

                                DOCUMENT
                                - 규정, 매뉴얼, 지침, 정책 등 문서 내용 질문

                                DATABASE
                                - 직원 수, 매출, 통계, 목록 등
                                  업무 DB의 정확한 데이터 조회가 필요한 질문

                                HYBRID
                                - 문서 내용과 업무 DB 데이터가 모두 필요한 질문

                                반드시 DOCUMENT, DATABASE, HYBRID 중
                                하나의 단어만 답변하세요.
                                """)
                        .user(question)
                        .call()
                        .content();

        return parseQuestionType(result);
    }

    /*
     * Gemini의 분류 결과를 서버에서 허용한 값으로만 변환한다.
     */
    private QuestionType parseQuestionType(
            String result
    ) {

        if (result == null) {
            throw new IllegalStateException(
                    "질문 유형 분석 결과가 없습니다."
            );
        }

        String normalized =
                result.trim().toUpperCase();

        return switch (normalized) {
            case "DOCUMENT" -> QuestionType.DOCUMENT;
            case "DATABASE" -> QuestionType.DATABASE;
            case "HYBRID" -> QuestionType.HYBRID;

            default -> throw new IllegalStateException(
                    "허용되지 않은 질문 유형입니다."
            );
        };
    }

    private void validateQuestion(
            String question
    ) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }
    }

    /*
     * 현재 프로젝트에서 지원하는 질문 처리 유형이다.
     */
    public enum QuestionType {
        DOCUMENT,
        DATABASE,
        HYBRID
    }
}