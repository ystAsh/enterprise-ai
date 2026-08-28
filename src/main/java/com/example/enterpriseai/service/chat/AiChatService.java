/*
 * =============================================================================
 * 클래스명 : AiChatService
 * =============================================================================
 * 목적
 *  - 로그인 사용자의 자연어 질문을 적절한 RAG 처리 경로로 전달한다.
 *  - DOCUMENT / DATABASE 처리 결과를 공통 ChatResponse 형태로 구성한다.
 *  - 소량 Database 결과만 Gemini와 채팅 화면에 직접 전달한다.
 *  - 대량 Database 결과는 전체 데이터를 Gemini와 채팅 응답에 전달하지 않는다.
 *  - 대량 Database 결과는 서버에 보관하고 opaque resultReference만 외부에 전달한다.
 *  - 전체 결과 조회 및 다운로드 가능 여부를 안전한 응답 정보로 제공한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.chat;

import com.example.enterpriseai.dto.ChatResponse;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.database.DatabaseQueryRequestService;
import com.example.enterpriseai.service.database.DatabaseRagService;
import com.example.enterpriseai.service.database.DatabaseResultPresentationPolicy;
import com.example.enterpriseai.service.database.DatabaseResultReferenceStore;
import com.example.enterpriseai.service.document.DocumentRagService;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final QuestionRouterService questionRouterService;
    private final DocumentRagService documentRagService;
    private final DatabaseQueryRequestService databaseQueryRequestService;
    private final DatabaseRagService databaseRagService;
    private final DatabaseResultPresentationPolicy presentationPolicy;
    private final DatabaseResultReferenceStore resultReferenceStore;

    public AiChatService(
            QuestionRouterService questionRouterService,
            DocumentRagService documentRagService,
            DatabaseQueryRequestService databaseQueryRequestService,
            DatabaseRagService databaseRagService,
            DatabaseResultPresentationPolicy presentationPolicy,
            DatabaseResultReferenceStore resultReferenceStore
    ) {
        this.questionRouterService =
                questionRouterService;

        this.documentRagService =
                documentRagService;

        this.databaseQueryRequestService =
                databaseQueryRequestService;

        this.databaseRagService =
                databaseRagService;

        this.presentationPolicy =
                presentationPolicy;

        this.resultReferenceStore =
                resultReferenceStore;
    }

    public String generateAnswer(
            String question,
            CurrentUser currentUser
    ) {

        return generateResponse(
                question,
                currentUser
        ).answer();
    }

    public ChatResponse generateResponse(
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
                    new ChatResponse(
                            documentRagService.answer(
                                    question,
                                    currentUser
                            )
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
     * 검증 완료 Database 결과의 크기에 따라
     * INLINE / EXTERNAL 처리 경로를 분리한다.
     */
    private ChatResponse answerDatabase(
            String question,
            CurrentUser currentUser
    ) {

        DatabaseQueryResult queryResult =
                databaseQueryRequestService.execute(
                        question,
                        currentUser
                );

        DatabaseResultPresentationPolicy.PresentationType presentationType =
                presentationPolicy.determine(
                        queryResult.metadata().returnedCount()
                );

        return switch (presentationType) {

            case INLINE ->
                    answerInlineDatabase(
                            question,
                            queryResult
                    );

            case EXTERNAL ->
                    answerExternalDatabase(
                            queryResult,
                            currentUser
                    );
        };
    }

    /*
     * 소량 결과:
     * 검증 완료 데이터만 Gemini와 React에 전달한다.
     */
    private ChatResponse answerInlineDatabase(
            String question,
            DatabaseQueryResult queryResult
    ) {

        String answer =
                databaseRagService.answer(
                        question,
                        queryResult
                );

        return new ChatResponse(
                answer,
                queryResult.data(),
                queryResult.metadata().totalCount(),
                queryResult.metadata().returnedCount(),
                false,
                null,
                false
        );
    }

    /*
     * 대량 결과:
     * 전체 업무 데이터는 Gemini와 ChatResponse.data에 전달하지 않는다.
     *
     * 검증 완료 결과는 서버 저장소에 보관하고
     * 외부에는 opaque resultReference만 전달한다.
     *
     * 전체 조회 및 CSV 다운로드는 resultReference를 이용하며,
     * 실제 접근 시 Spring Security 사용자 소유권을 다시 검증한다.
     */
    private ChatResponse answerExternalDatabase(
            DatabaseQueryResult queryResult,
            CurrentUser currentUser
    ) {

        int returnedCount =
                queryResult.metadata().returnedCount();

        String resultReference =
                resultReferenceStore.save(
                        queryResult,
                        currentUser
                );

        String answer =
                returnedCount
                        + "건의 조회 결과가 있습니다. "
                        + "전체 결과를 조회하거나 파일로 다운로드할 수 있습니다.";

        return new ChatResponse(
                answer,
                null,
                queryResult.metadata().totalCount(),
                returnedCount,
                true,
                resultReference,
                true
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