/*
 * =============================================================================
 * 클래스명 : DocumentRagService
 * =============================================================================
 * 목적
 *  - 사용자의 자연어 질문에 대해 권한 기반 Document RAG를 수행한다.
 *  - 권한 검증이 완료된 문서 Chunk만 최소 Context로 구성한다.
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
        this.documentVectorSearchService =
                documentVectorSearchService;

        this.contextBuilder =
                contextBuilder;

        this.chatClient =
                chatClientBuilder.build();
    }

    /*
     * 로그인 사용자의 권한 범위 안에서 문서를 검색하고,
     * 검증된 문서 내용만 이용하여 Gemini 답변을 생성한다.
     */
    public String answer(
            String question,
            CurrentUser currentUser
    ) {

        validateQuestion(question);

        // 권한 Filter + 검색 결과 재검증이 적용된 Vector Search
        List<Document> documents =
                documentVectorSearchService.search(
                        question,
                        currentUser
                );

        // 접근 가능한 관련 문서가 없으면 Gemini를 호출하지 않는다.
        if (documents.isEmpty()) {
            return "현재 접근 가능한 정보에서 관련 내용을 찾을 수 없습니다.";
        }

        // 검증 완료된 Chunk에서 문서 내용만 추출한다.
        String context =
                contextBuilder.build(
                        documents
                );

        if (context.isBlank()) {
            return "현재 접근 가능한 정보에서 관련 내용을 찾을 수 없습니다.";
        }

        // Gemini에는 사용자 권한정보나 내부 Metadata를 전달하지 않는다.
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

    /*
     * Gemini 호출 전에 사용자 질문을 최소 검증한다.
     */
    private void validateQuestion(
            String question
    ) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }
    }
}