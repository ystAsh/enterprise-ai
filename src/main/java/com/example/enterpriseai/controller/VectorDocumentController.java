/*
 * =============================================================================
 * 클래스명 : VectorDocumentController
 * =============================================================================
 * 목적
 *  - 로그인 사용자의 문서 업로드 및 Document RAG 관련 HTTP 요청을 처리한다.
 *  - Spring Security가 인증한 CurrentUser를 서버에서 직접 가져온다.
 *  - Database RAG 결과와 사용자에게 공개 가능한 안전한 조회 근거를 반환한다.
 */

package com.example.enterpriseai.controller;

import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.entity.VectorDocument;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.database.DatabaseQueryService;
import com.example.enterpriseai.service.database.DatabaseRagService;
import com.example.enterpriseai.service.document.DocumentRagService;
import com.example.enterpriseai.service.document.VectorDocumentService;
import com.example.enterpriseai.service.vector.DocumentVectorSearchService;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


// 문서 관리 REST API를 제공하는 Controller로 등록한다.
@RestController

// 문서 관련 API의 기본 URL이다.
@RequestMapping("/api/documents")
public class VectorDocumentController {

    private final VectorDocumentService vectorDocumentService;
    private final DocumentVectorSearchService documentVectorSearchService;
    private final DocumentRagService documentRagService;
    private final DatabaseQueryService databaseQueryService;
    private final DatabaseRagService databaseRagService;

    public VectorDocumentController(
            VectorDocumentService vectorDocumentService,
            DocumentVectorSearchService documentVectorSearchService,
            DocumentRagService documentRagService,
            DatabaseQueryService databaseQueryService,
            DatabaseRagService databaseRagService
    ) {
        this.vectorDocumentService = vectorDocumentService;
        this.documentVectorSearchService = documentVectorSearchService;
        this.documentRagService = documentRagService;
        this.databaseQueryService = databaseQueryService;
        this.databaseRagService = databaseRagService;
    }

    // 로그인 사용자의 문서 업로드 요청을 처리한다.
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UploadResponse> upload(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam("securityLevel") int securityLevel
    ) {

        VectorDocument document =
                vectorDocumentService.upload(
                        file,
                        securityLevel,
                        currentUser
                );

        UploadResponse response =
                new UploadResponse(
                        document.getDocumentId(),
                        document.getOriginalFileName(),
                        document.getSecurityLevel(),
                        document.getStatus().name()
                );

        return ResponseEntity.ok(response);
    }

    /*
     * 업로드된 PDF 문서의 텍스트 추출 결과를 확인한다.
     *
     * 현재 Phase 5 Parser 검증을 위한 임시 API이다.
     * 실제 Document RAG에서는 전체 원문을 클라이언트에 반환하지 않는다.
     */
    @GetMapping("/{documentId}/parse")
    public ResponseEntity<ParseResponse> parsePdf(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        String text =
                vectorDocumentService.parsePdf(
                        documentId,
                        currentUser
                );

        return ResponseEntity.ok(
                new ParseResponse(
                        documentId,
                        text
                )
        );
    }

    /*
     * 업로드된 PDF 문서의 Chunk 분할 결과와 Metadata를 확인한다.
     *
     * 현재 Phase 6 Chunk 검증을 위한 임시 API이다.
     * 실제 Document RAG에서는 Chunk 전체를 클라이언트에 반환하지 않는다.
     */
    @GetMapping("/{documentId}/chunks")
    public ResponseEntity<ChunkResponse> chunks(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        List<Document> chunks =
                vectorDocumentService.chunkPdf(
                        documentId,
                        currentUser
                );

        return ResponseEntity.ok(
                new ChunkResponse(
                        documentId,
                        chunks.size(),
                        chunks
                )
        );
    }

    /*
     * 업로드된 PDF 문서를 실제로 Embedding하고
     * PGVector에 저장되는지 확인한다.
     *
     * 현재 Phase 7 Embedding 검증을 위한 임시 API이다.
     */
    @PostMapping("/{documentId}/embed")
    public ResponseEntity<EmbedResponse> embed(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        int chunkCount =
                vectorDocumentService.embedPdf(
                        documentId,
                        currentUser
                );

        return ResponseEntity.ok(
                new EmbedResponse(
                        documentId,
                        chunkCount
                )
        );
    }

    /*
     * =============================================================================
     * Vector Search 검증 API
     * =============================================================================
     * 목적
     *  - 로그인 사용자의 권한 범위 안에서만 Vector Search가 수행되는지 확인한다.
     *  - 현재 Phase 8 검증용 임시 API이다.
     */
    @GetMapping("/search")
    public ResponseEntity<VectorSearchResponse> search(
            @RequestParam("query") String query,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        List<Document> documents =
                documentVectorSearchService.search(
                        query,
                        currentUser
                );

        return ResponseEntity.ok(
                new VectorSearchResponse(
                        query,
                        documents.size(),
                        documents
                )
        );
    }

    /*
     * =============================================================================
     * Document RAG 검증 API
     * =============================================================================
     * 목적
     *  - 로그인 사용자의 자연어 질문을 Document RAG로 처리한다.
     *  - 권한 검증된 문서 Chunk만 Gemini Context로 사용한다.
     */
    @GetMapping("/rag")
    public ResponseEntity<DocumentRagResponse> rag(
            @RequestParam("question") String question,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        String answer =
                documentRagService.answer(
                        question,
                        currentUser
                );

        return ResponseEntity.ok(
                new DocumentRagResponse(
                        question,
                        answer
                )
        );
    }

    /*
     * =============================================================================
     * Database RAG 검증 API
     * =============================================================================
     * 목적
     *  - 현재 Phase 10 Database RAG 연결 상태를 검증한다.
     *  - 조회 결과를 공통 DatabaseQueryResult로 전달한다.
     *  - DatabaseRagService는 검증 완료 결과를 기반으로 답변만 생성한다.
     *  - 사용자에게 공개 가능한 안전한 evidence만 반환한다.
     *
     * 주의
     *  - 현재 employee-count URL과 Query 호출은 기존 Phase 10 테스트용이다.
     *  - 공통 AI/RAG 검증 계층의 업무 모델로 사용하지 않는다.
     */
    @GetMapping("/database/rag/employee-count")
    public ResponseEntity<DatabaseRagResponse> databaseRagEmployeeCount(
            @RequestParam("question") String question,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        DatabaseQueryResult queryResult =
                databaseQueryService
                        .getCurrentDepartmentEmployeeCount(
                                currentUser
                        );

        String answer =
                databaseRagService.answer(
                        question,
                        queryResult
                );

        return ResponseEntity.ok(
                new DatabaseRagResponse(
                        question,
                        answer,
                        queryResult.evidence()
                )
        );
    }

    /*
     * 업로드 성공 시 클라이언트에 반환할 최소 정보이다.
     *
     * storagePath, ownerId, organizationId 등의 내부 정보는
     * 불필요하게 브라우저에 반환하지 않는다.
     */
    public record UploadResponse(
            Long documentId,
            String fileName,
            int securityLevel,
            String status
    ) {
    }

    /*
     * PDF Parser 테스트 결과를 반환한다.
     */
    public record ParseResponse(
            Long documentId,
            String text
    ) {
    }

    /*
     * Chunk 테스트 결과를 반환한다.
     *
     * 현재 Phase 6 검증용이며 실제 Document RAG 응답에는 사용하지 않는다.
     */
    public record ChunkResponse(
            Long documentId,
            int chunkCount,
            List<Document> chunks
    ) {
    }

    /*
     * Embedding 테스트 결과를 반환한다.
     */
    public record EmbedResponse(
            Long documentId,
            int chunkCount
    ) {
    }

    /*
     * Vector Search 테스트 결과를 반환한다.
     *
     * 실제 Document RAG에서는 Chunk 전체를
     * 브라우저에 그대로 반환하지 않는다.
     */
    public record VectorSearchResponse(
            String query,
            int resultCount,
            List<Document> documents
    ) {
    }

    /*
     * Document RAG 테스트 결과를 반환한다.
     */
    public record DocumentRagResponse(
            String question,
            String answer
    ) {
    }

    /*
     * Database RAG 테스트 결과를 반환한다.
     *
     * evidence에는 SQL 원문이나 내부 권한 값이 아닌
     * 사용자에게 공개 가능한 안전한 조회 근거만 포함한다.
     */
    public record DatabaseRagResponse(
            String question,
            String answer,
            DatabaseQueryResult.Evidence evidence
    ) {
    }
}