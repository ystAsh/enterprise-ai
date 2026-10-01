/*
 * =============================================================================
 * 클래스명 : HybridRagService
 * =============================================================================
 * 목적
 *  - Hybrid 질문을 Document용 질문과 Database용 질문으로 분리한다.
 *  - Document와 Database를 각각 독립적으로 검증한다.
 *  - 검증 완료된 최소 Context만 결합하여 Gemini 최종 답변을 생성한다.
 *  - 대량 Database 결과 전체를 Gemini에 전달하지 않고 resultReference로 분리한다.
 *  - Hybrid 처리 중 사용자에게 노출 가능한 진행 상태를 ChatProgressReporter를 통해 전달한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.hybrid;

import com.example.enterpriseai.dto.ChatProgressEvent;
import com.example.enterpriseai.dto.ChatResponse;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.chat.ChatProgressReporter;
import com.example.enterpriseai.service.database.DatabaseQueryRequestService;
import com.example.enterpriseai.service.database.DatabaseResultPresentationPolicy;
import com.example.enterpriseai.service.database.DatabaseResultReferenceStore;
import com.example.enterpriseai.service.document.DocumentRagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class HybridRagService {

    private final HybridQuestionDecomposer questionDecomposer;
    private final DocumentRagService documentRagService;
    private final DatabaseQueryRequestService databaseQueryRequestService;
    private final DatabaseResultPresentationPolicy presentationPolicy;
    private final DatabaseResultReferenceStore resultReferenceStore;
    private final ChatClient chatClient;

    public HybridRagService(
            HybridQuestionDecomposer questionDecomposer,
            DocumentRagService documentRagService,
            DatabaseQueryRequestService databaseQueryRequestService,
            DatabaseResultPresentationPolicy presentationPolicy,
            DatabaseResultReferenceStore resultReferenceStore,
            ChatClient.Builder chatClientBuilder
    ) {
        this.questionDecomposer = questionDecomposer;
        this.documentRagService = documentRagService;
        this.databaseQueryRequestService = databaseQueryRequestService;
        this.presentationPolicy = presentationPolicy;
        this.resultReferenceStore = resultReferenceStore;
        this.chatClient = chatClientBuilder.build();
    }

    // 기존 동기 호출은 Progress 없이 동일하게 처리한다.
    public ChatResponse generateResponse(
            String question,
            CurrentUser currentUser
    ) {
        return generateResponse(
                question,
                currentUser,
                ChatProgressReporter.NO_OP
        );
    }

    // Hybrid 질문을 분리하고 검증 완료된 Document/Database 결과를 결합한다.
    public ChatResponse generateResponse(
            String question,
            CurrentUser currentUser,
            ChatProgressReporter progressReporter
    ) {
        validateInput(question, currentUser, progressReporter);

        HybridQuestionDecomposer.HybridQuestions questions =
                questionDecomposer.decompose(question);

        String documentContext =
                documentRagService.buildValidatedContext(
                        questions.documentQuestion(),
                        currentUser,
                        progressReporter
                );

        progressReporter.report(
                new ChatProgressEvent(
                        ChatProgressEvent.Stage.DATABASE_SEARCH,
                        "사내 데이터 조회 중",
                        "database"
                )
        );

        DatabaseQueryResult databaseResult =
                databaseQueryRequestService.execute(
                        questions.databaseQuestion(),
                        currentUser
                );

        DatabaseResultPresentationPolicy.PresentationType presentationType =
                presentationPolicy.determine(
                        databaseResult.metadata().returnedCount()
                );

        return switch (presentationType) {
            case INLINE ->
                    buildInlineResponse(
                            question,
                            documentContext,
                            databaseResult,
                            progressReporter
                    );

            case EXTERNAL ->
                    buildExternalResponse(
                            question,
                            documentContext,
                            databaseResult,
                            currentUser,
                            progressReporter
                    );
        };
    }

    // 소량 DB 결과는 검증 완료 데이터와 Document Context를 함께 사용한다.
    private ChatResponse buildInlineResponse(
            String question,
            String documentContext,
            DatabaseQueryResult databaseResult,
            ChatProgressReporter progressReporter
    ) {
        String answer = generateAnswer(
                question,
                documentContext,
                databaseResult.data().toString(),
                progressReporter
        );

        return new ChatResponse(
                answer,
                databaseResult.data(),
                databaseResult.metadata().totalCount(),
                databaseResult.metadata().returnedCount(),
                false,
                null,
                false
        );
    }

    // 대량 DB 결과는 전체 Rows를 Gemini에 전달하지 않고 reference로 분리한다.
    private ChatResponse buildExternalResponse(
            String question,
            String documentContext,
            DatabaseQueryResult databaseResult,
            CurrentUser currentUser,
            ChatProgressReporter progressReporter
    ) {
        int returnedCount =
                databaseResult.metadata().returnedCount();

        String resultReference =
                resultReferenceStore.save(
                        databaseResult,
                        currentUser
                );

        String databaseContext =
                "검증 완료 Database 조회 결과는 "
                        + returnedCount
                        + "건이며 전체 상세 데이터는 별도 결과로 제공됩니다.";

        String answer = generateAnswer(
                question,
                documentContext,
                databaseContext,
                progressReporter
        );

        return new ChatResponse(
                answer,
                null,
                databaseResult.metadata().totalCount(),
                returnedCount,
                true,
                resultReference,
                true
        );
    }

    // 검증 완료된 두 Context만 이용해 최종 Hybrid 답변을 생성한다.
    private String generateAnswer(
            String question,
            String documentContext,
            String databaseContext,
            ChatProgressReporter progressReporter
    ) {
        progressReporter.report(
                new ChatProgressEvent(
                        ChatProgressEvent.Stage.ANSWER_GENERATION,
                        "답변 생성 중",
                        "answer"
                )
        );

        return chatClient.prompt()
                .system("""
                        검증 완료된 Document와 Database Context만 근거로 답변하세요.

                        Context에 없는 사실을 추측하거나 생성하지 마세요.
                        사용자의 원래 질문에 맞게 두 근거를 자연스럽게 결합하세요.
                        Database Context가 건수 정보만 제공하는 경우
                        상세 목록을 임의로 만들어내지 마세요.

                        내부 SQL, Query 정보, 권한정보,
                        시스템 구현 구조는 설명하지 마세요.
                        """)
                .user("""
                        [원래 사용자 질문]
                        %s

                        [검증 완료 Document Context]
                        %s

                        [검증 완료 Database Context]
                        %s
                        """.formatted(
                        question,
                        normalizeContext(documentContext),
                        normalizeContext(databaseContext)
                ))
                .call()
                .content();
    }

    private String normalizeContext(String context) {
        if (context == null || context.isBlank()) {
            return "관련 검증 완료 Context 없음";
        }

        return context.trim();
    }

    private void validateInput(
            String question,
            CurrentUser currentUser,
            ChatProgressReporter progressReporter
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

        if (progressReporter == null) {
            throw new IllegalArgumentException(
                    "progressReporter must not be null"
            );
        }
    }
}