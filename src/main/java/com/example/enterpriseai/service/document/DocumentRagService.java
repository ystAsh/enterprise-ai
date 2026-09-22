/*
 * =============================================================================
 * 클래스명 : DocumentRagService
 * =============================================================================
 * 목적
 *  - 사용자의 자연어 질문에 대해 권한 기반 Document RAG를 수행한다.
 *  - 권한 검증이 완료된 문서 Chunk만 최소 Context로 구성한다.
 *  - 검증 완료 Context를 Document RAG와 Hybrid RAG에서 재사용할 수 있게 제공한다.
 *  - 검증된 Context와 사용자 질문을 Gemini에 전달하여 최종 답변을 생성한다.
 */

package com.example.enterpriseai.service.document;

import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.vector.DocumentVectorSearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentRagService {

    private final DocumentVectorSearchService documentVectorSearchService;
    private final DocumentRagContextBuilder contextBuilder;
    private final ChatClient chatClient;

    public DocumentRagService(
            DocumentVectorSearchService documentVectorSearchService,
            DocumentRagContextBuilder contextBuilder,
            ChatClient.Builder chatClientBuilder
    ) {
        this.documentVectorSearchService = documentVectorSearchService;
        this.contextBuilder = contextBuilder;
        this.chatClient = chatClientBuilder.build();
    }

    // 검증 완료 Document Context를 이용해 Gemini 답변을 생성한다.
    public String answer(
            String question,
            CurrentUser currentUser
    ) {
        String context = buildValidatedContext(question, currentUser);

        if (context.isBlank()) {
            return "현재 접근 가능한 정보에서 관련 내용을 찾을 수 없습니다.";
        }

        return chatClient.prompt()
                .system("""
                        당신은 사내 문서 질의응답 AI입니다.
                        반드시 제공된 문서 내용만 근거로 답변하세요.
                        문서에 없는 내용을 추측하거나 만들어내지 마세요.
                        답을 확인할 수 없다면
                        "제공된 문서에서 확인할 수 없습니다."라고 답변하세요.

                        답변은 사용자의 질문에 필요한 내용만
                        간결하고 명확하게 작성하세요.
                        """)
                .user("""
                        [사용자 질문]
                        %s

                        [검증된 문서 내용]
                        %s
                        """.formatted(
                        question,
                        context
                ))
                .call()
                .content();
    }

    // 권한 검증이 완료된 문서 Chunk만 최소 Context로 구성한다.
    public String buildValidatedContext(
            String question,
            CurrentUser currentUser
    ) {
        validateInput(question, currentUser);

        List<Document> documents =
                documentVectorSearchService.search(
                        question,
                        currentUser
                );

        if (documents.isEmpty()) {
            return "";
        }

        String context = contextBuilder.build(documents);

        return context == null
                ? ""
                : context.trim();
    }

    // Gemini 호출 전에 질문과 인증 사용자를 확인한다.
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