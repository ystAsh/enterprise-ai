/*
 * =============================================================================
 * 클래스명 : AiChatService
 * =============================================================================
 * 목적
 *  - 로그인 사용자의 자연어 질문을 적절한 RAG 처리 경로로 전달한다.
 *  - Question Router의 DOCUMENT / DATABASE 분류 결과에 따라
 *    검증된 Document RAG 또는 Database RAG를 호출한다.
 *  - HYBRID는 현재 Phase에서 실행하지 않는다.
 */

package com.example.enterpriseai.service.chat;

import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.database.DatabaseQueryRequestService;
import com.example.enterpriseai.service.database.DatabaseRagService;
import com.example.enterpriseai.service.document.DocumentRagService;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final QuestionRouterService questionRouterService;
    private final DocumentRagService documentRagService;
    private final DatabaseQueryRequestService databaseQueryRequestService;
    private final DatabaseRagService databaseRagService;

    public AiChatService(
            QuestionRouterService questionRouterService,
            DocumentRagService documentRagService,
            DatabaseQueryRequestService databaseQueryRequestService,
            DatabaseRagService databaseRagService
    ) {
        this.questionRouterService =
                questionRouterService;

        this.documentRagService =
                documentRagService;

        this.databaseQueryRequestService =
                databaseQueryRequestService;

        this.databaseRagService =
                databaseRagService;
    }

    /*
     * 질문을 Router로 분류하고 현재 지원되는 RAG 경로로 전달한다.
     */
    public String generateAnswer(
            String question,
            CurrentUser currentUser
    ) {

        validateInput(
                question,
                currentUser
        );

        QuestionRouterService.QuestionType questionType =
                questionRouterService.route(
                        question
                );

        return switch (questionType) {

            case DOCUMENT ->
                    documentRagService.answer(
                            question,
                            currentUser
                    );

            case DATABASE ->
                    answerDatabase(
                            question,
                            currentUser
                    );

            case HYBRID ->
                    throw new UnsupportedOperationException(
                            "HYBRID RAG는 현재 지원하지 않습니다."
                    );
        };
    }

    /*
     * Database Query 실행 결과는 검증 완료된 DatabaseQueryResult만
     * DatabaseRagService에 전달한다.
     */
    private String answerDatabase(
            String question,
            CurrentUser currentUser
    ) {

        DatabaseQueryResult queryResult =
                databaseQueryRequestService.execute(
                        question,
                        currentUser
                );

        return databaseRagService.answer(
                question,
                queryResult
        );
    }

    private void validateInput(
            String question,
            CurrentUser currentUser
    ) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }

        if (currentUser == null) {
            throw new SecurityException(
                    "인증된 사용자가 없습니다."
            );
        }
    }
}