/*
 * =============================================================================
 * 클래스명 : VectorDocumentController
 * =============================================================================
 * 목적
 *  - 로그인 사용자의 문서 업로드 HTTP 요청을 처리한다.
 *  - Spring Security가 인증한 CurrentUser를 서버에서 직접 가져온다.
 *  - VectorDocumentService를 호출하여 파일 저장 및 MSSQL 문서 정보를 등록한다.
 */

package com.example.enterpriseai.controller;

import com.example.enterpriseai.entity.VectorDocument;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.document.VectorDocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

// 문서 관리 REST API를 제공하는 Controller로 등록한다.
@RestController

// 문서 관련 API의 기본 URL이다.
@RequestMapping("/api/documents")
public class VectorDocumentController {

    private final VectorDocumentService vectorDocumentService;

    public VectorDocumentController(
            VectorDocumentService vectorDocumentService
    ) {
        this.vectorDocumentService = vectorDocumentService;
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

        UploadResponse response = new UploadResponse(
                document.getDocumentId(),
                document.getOriginalFileName(),
                document.getSecurityLevel(),
                document.getStatus().name()
        );

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/documents/"
                                        + document.getDocumentId()
                        )
                )
                .body(response);
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
}