/*
 * =============================================================================
 * 클래스명 : HybridRagService
 * =============================================================================
 * 목적
 *  - Document와 Database의 검증 완료 결과를 결합하여 Hybrid RAG 답변을 생성한다.
 *  - Document와 Database를 각각 독립적으로 검증한 뒤 최소 Context만 Gemini에 전달한다.
 *  - 대량 Database 결과 전체를 Gemini에 전달하지 않는다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.hybrid;

import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.database.DatabaseQueryRequestService;
import com.example.enterpriseai.service.database.DatabaseResultPresentationPolicy;
import com.example.enterpriseai.service.document.DocumentRagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class HybridRagService {

    private final DocumentRagService documentRagService;
    private final DatabaseQueryRequestService databaseQueryRequestService;
    private final DatabaseResultPresentationPolicy presentationPolicy;
    private final ChatClient chatClient;

    public HybridRagService(
            DocumentRagService documentRagService,
            DatabaseQueryRequestService databaseQueryRequestService,
            DatabaseResultPresentationPolicy presentationPolicy,
            ChatClient.Builder chatClientBuilder
    ) {
        this.documentRagService = documentRagService;
        this.databaseQueryRequestService = databaseQueryRequestService;
        this.presentationPolicy = presentationPolicy;
        this.chatClient = chatClientBuilder.build();
    }

    // Document와 Database를 독립 검증하고 최소 Context만 결합한다.
    public String answer(
            String question,
            CurrentUser currentUser
    ) {
        validateInput(question, currentUser);

        String documentContext =
                documentRagService.buildValidatedContext(
                        question,
                        currentUser
                );

        DatabaseQueryResult databaseResult =
                databaseQueryRequestService.execute(
                        question,
                        currentUser
                );

        String databaseContext =
                buildDatabaseContext(databaseResult);

        return chatClient.prompt()
                .system("""
                        당신은 검증 완료된 사내 문서와 업무 데이터만
                        근거로 답변하는 AI입니다.

                        제공된 Context에 없는 사실을 추측하거나
                        생성하지 마세요.

                        Document와 Database의 내용이 모두 필요한 경우
                        두 근거를 함께 사용하세요.

                        내부 SQL, Query 정보, 권한정보,
                        시스템 구현 구조를 설명하지 마세요.

                        사용자의 질문에 필요한 내용만
                        간결하고 자연스럽게 답변하세요.
                        """)
                .user("""
                        [사용자 질문]
                        %s

                        [검증 완료 Document Context]
                        %s

                        [검증 완료 Database Context]
                        %s
                        """.formatted(
                        question,
                        documentContext,
                        databaseContext
                ))
                .call()
                .content();
    }

    // 소량 결과만 상세 Context로 사용하고 대량 결과는 건수 정보만 전달한다.
    private String buildDatabaseContext(
            DatabaseQueryResult queryResult
    ) {
        DatabaseResultPresentationPolicy.PresentationType presentationType =
                presentationPolicy.determine(
                        queryResult.metadata().returnedCount()
                );

        if (presentationType
                == DatabaseResultPresentationPolicy.PresentationType.EXTERNAL) {

            return "검증 완료 조회 결과 건수: "
                    + queryResult.metadata().returnedCount();
        }

        return queryResult.data().toString();
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