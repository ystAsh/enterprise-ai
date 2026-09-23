/*
 * =============================================================================
 * 클래스명 : HybridQuestionDecomposer
 * =============================================================================
 * 목적
 *  - HYBRID 질문을 Document용 질문과 Database용 질문으로 분리한다.
 *  - 원래 질문에 없는 업무 사실이나 조건을 임의로 생성하지 않는다.
 *  - 분리된 질문은 각각 독립적인 Document/Database 검증 경로에서 사용한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.hybrid;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class HybridQuestionDecomposer {

    private final ChatClient chatClient;

    public HybridQuestionDecomposer(
            ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient = chatClientBuilder.build();
    }

    public HybridQuestions decompose(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }

        HybridQuestions questions = chatClient.prompt()
                .system("""
                        사용자의 HYBRID 질문을 두 개의 독립적인 질문으로 분리하세요.

                        documentQuestion:
                        사내 문서, 정책, 규정, 매뉴얼 등에서 확인할 질문

                        databaseQuestion:
                        정형 업무 데이터 조회가 필요한 질문

                        원래 질문에 없는 조건이나 사실을 추가하지 마세요.
                        SQL, Schema, 테이블, 컬럼, queryKey를 생성하지 마세요.
                        두 질문 모두 원래 사용자의 의도를 유지해야 합니다.
                        """)
                .user("""
                        [원래 질문]
                        %s
                        """.formatted(question))
                .call()
                .entity(
                        HybridQuestions.class,
                        spec -> spec.useProviderStructuredOutput()
                );

        validateResult(questions);

        return questions;
    }

    // 분해 결과가 두 독립 경로에서 사용할 수 있는지 확인한다.
    private void validateResult(HybridQuestions questions) {
        if (questions == null) {
            throw new IllegalStateException(
                    "Hybrid 질문 분해 결과가 없습니다."
            );
        }

        if (questions.documentQuestion() == null
                || questions.documentQuestion().isBlank()) {
            throw new IllegalStateException(
                    "Document 질문이 없습니다."
            );
        }

        if (questions.databaseQuestion() == null
                || questions.databaseQuestion().isBlank()) {
            throw new IllegalStateException(
                    "Database 질문이 없습니다."
            );
        }
    }

    public record HybridQuestions(
            String documentQuestion,
            String databaseQuestion
    ) {
    }
}