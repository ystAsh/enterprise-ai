/*
 * =============================================================================
 * 클래스명 : AiChatService
 * =============================================================================
 * 목적
 *  - 로그인 사용자의 자연어 질문을 적절한 RAG 처리 경로로 전달한다.
 *  - DOCUMENT / DATABASE / HYBRID 처리 결과를 공통 ChatResponse 형태로 구성한다.
 *  - 소량 Database 결과만 Gemini와 채팅 화면에 직접 전달한다.
 *  - 대량 Database 결과는 전체 데이터를 Gemini와 채팅 응답에 전달하지 않는다.
 *  - 대량 Database 결과는 서버에 보관하고 opaque resultReference만 외부에 전달한다.
 *  - Database 실행 근거와 실제 답변을 Secure Verification Evidence로 저장한다.
 *  - INLINE Database 답변은 검증 완료 Evidence 기준으로 deterministic Answer Verification을 수행한다.
 *  - Answer Verification 결과를 Evaluation DB에 저장한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.chat;

import com.example.enterpriseai.dto.AnswerVerificationPolicy;
import com.example.enterpriseai.dto.AnswerVerificationResult;
import com.example.enterpriseai.dto.ChatResponse;
import com.example.enterpriseai.dto.DatabaseQueryExecutionContext;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.dto.SecureVerificationEvidence;
import com.example.enterpriseai.dto.SecureVerificationEvidencePolicy;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.database.*;
import com.example.enterpriseai.service.document.DocumentRagService;
import com.example.enterpriseai.service.evaluation.AnswerVerificationResultPersistenceService;
import com.example.enterpriseai.service.evaluation.AnswerVerificationService;
import com.example.enterpriseai.service.hybrid.HybridRagService;
import com.example.enterpriseai.service.security.SecureVerificationEvidencePolicyProvider;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final QuestionRouterService questionRouterService;
    private final DocumentRagService documentRagService;
    private final DatabaseQueryRequestService databaseQueryRequestService;
    private final DatabaseRagService databaseRagService;
    private final DatabaseResultPresentationPolicy presentationPolicy;
    private final DatabaseResultReferenceStore resultReferenceStore;
    private final HybridRagService hybridRagService;
    private final SecureVerificationEvidenceAssembler secureEvidenceAssembler;
    private final SecureVerificationEvidencePersistenceService secureEvidencePersistenceService;
    private final SecureVerificationEvidencePolicyProvider secureEvidencePolicyProvider;
    private final AnswerVerificationService answerVerificationService;
    private final AnswerVerificationResultPersistenceService answerVerificationPersistenceService;

    public AiChatService(
            QuestionRouterService questionRouterService,
            DocumentRagService documentRagService,
            DatabaseQueryRequestService databaseQueryRequestService,
            DatabaseRagService databaseRagService,
            DatabaseResultPresentationPolicy presentationPolicy,
            DatabaseResultReferenceStore resultReferenceStore,
            HybridRagService hybridRagService,
            SecureVerificationEvidenceAssembler secureEvidenceAssembler,
            SecureVerificationEvidencePersistenceService secureEvidencePersistenceService,
            SecureVerificationEvidencePolicyProvider secureEvidencePolicyProvider,
            AnswerVerificationService answerVerificationService,
            AnswerVerificationResultPersistenceService answerVerificationPersistenceService
    ) {
        this.questionRouterService = questionRouterService;
        this.documentRagService = documentRagService;
        this.databaseQueryRequestService = databaseQueryRequestService;
        this.databaseRagService = databaseRagService;
        this.presentationPolicy = presentationPolicy;
        this.resultReferenceStore = resultReferenceStore;
        this.hybridRagService = hybridRagService;
        this.secureEvidenceAssembler = secureEvidenceAssembler;
        this.secureEvidencePersistenceService = secureEvidencePersistenceService;
        this.secureEvidencePolicyProvider = secureEvidencePolicyProvider;
        this.answerVerificationService = answerVerificationService;
        this.answerVerificationPersistenceService = answerVerificationPersistenceService;
    }

    public String generateAnswer(
            String question,
            CurrentUser currentUser
    ) {
        return generateResponse(question, currentUser).answer();
    }

    public ChatResponse generateResponse(
            String question,
            CurrentUser currentUser
    ) {
        validateInput(question, currentUser);

        QuestionRouterService.QuestionType questionType =
                questionRouterService.route(question);

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
                    hybridRagService.generateResponse(
                            question,
                            currentUser
                    );
        };
    }

    // 검증 완료 Database 결과 크기에 따라 INLINE / EXTERNAL 경로를 분리한다.
    private ChatResponse answerDatabase(
            String question,
            CurrentUser currentUser
    ) {
        DatabaseQueryExecutionContext executionContext =
                databaseQueryRequestService.executeWithContext(
                        question,
                        currentUser
                );

        DatabaseQueryResult queryResult =
                executionContext.queryResult();

        DatabaseResultPresentationPolicy.PresentationType presentationType =
                presentationPolicy.determine(
                        queryResult.metadata().returnedCount()
                );

        return switch (presentationType) {
            case INLINE ->
                    answerInlineDatabase(
                            question,
                            executionContext
                    );

            case EXTERNAL ->
                    answerExternalDatabase(
                            question,
                            executionContext,
                            currentUser
                    );
        };
    }

    // 소량 결과는 Gemini 답변 생성 후 Secure Evidence 저장과 deterministic 검증을 수행한다.
    private ChatResponse answerInlineDatabase(
            String question,
            DatabaseQueryExecutionContext executionContext
    ) {
        DatabaseQueryResult queryResult =
                executionContext.queryResult();

        DatabaseRagService.DatabaseRagAnswer ragAnswer =
                databaseRagService.answerWithContext(
                        question,
                        queryResult
                );

        Long verificationEvidenceId =
                saveSecureEvidence(
                        question,
                        executionContext,
                        SecureVerificationEvidencePolicy.ResultStorageMode.SNAPSHOT,
                        null,
                        ragAnswer.llmContext(),
                        ragAnswer.answer()
                );

        AnswerVerificationResult verificationResult =
                answerVerificationService.verify(
                        queryResult.data(),
                        ragAnswer.answer(),
                        AnswerVerificationPolicy.noneRequired()
                );

        answerVerificationPersistenceService.save(
                verificationEvidenceId,
                verificationResult
        );

        return new ChatResponse(
                ragAnswer.answer(),
                queryResult.data(),
                queryResult.metadata().totalCount(),
                queryResult.metadata().returnedCount(),
                false,
                null,
                false,
                verificationResult.overallStatus(),
                verificationResult.matchRate()
        );
    }

    // 대량 결과는 Raw Result를 Evidence에 저장하지 않고 resultReference로 연결한다.
    private ChatResponse answerExternalDatabase(
            String question,
            DatabaseQueryExecutionContext executionContext,
            CurrentUser currentUser
    ) {
        DatabaseQueryResult queryResult =
                executionContext.queryResult();

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

        saveSecureEvidence(
                question,
                executionContext,
                SecureVerificationEvidencePolicy.ResultStorageMode.REFERENCE_ONLY,
                resultReference,
                null,
                answer
        );

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

    // 실제 실행 정보와 최종 응답을 저장하고 Secure Verification Evidence ID를 반환한다.
    private Long saveSecureEvidence(
            String question,
            DatabaseQueryExecutionContext executionContext,
            SecureVerificationEvidencePolicy.ResultStorageMode resultStorageMode,
            String resultReference,
            String llmContext,
            String finalAnswer
    ) {
        DatabaseQueryResult queryResult =
                executionContext.queryResult();

        SecureVerificationEvidencePolicy policy =
                secureEvidencePolicyProvider.create(
                        executionContext.executedParameters().keySet(),
                        resultStorageMode,
                        presentationPolicy.inlineMaxRows()
                );

        SecureVerificationEvidence evidence =
                secureEvidenceAssembler.assemble(
                        question,
                        executionContext.source(),
                        executionContext.queryKey(),
                        executionContext.executionType(),
                        executionContext.queryEvidence(),
                        executionContext.executedParameters(),
                        queryResult.data(),
                        queryResult.metadata().returnedCount(),
                        resultReference,
                        null,
                        llmContext,
                        finalAnswer,
                        null,
                        executionContext.executedAt(),
                        policy
                );

        return secureEvidencePersistenceService.save(
                evidence,
                policy
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