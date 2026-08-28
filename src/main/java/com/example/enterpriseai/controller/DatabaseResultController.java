/*
 * =============================================================================
 * 클래스명 : DatabaseResultController
 * =============================================================================
 * 목적
 *  - 대량 Database RAG 결과를 resultReference로 조회한다.
 *  - Spring Security가 인증한 현재 사용자만 자신의 결과에 접근하도록 한다.
 *  - 내부 Query 정보나 권한 정보를 외부에 노출하지 않는다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.controller;

import com.example.enterpriseai.dto.DatabaseResultReference;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.database.DatabaseResultReferenceStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/database/results")
public class DatabaseResultController {

    private final DatabaseResultReferenceStore resultReferenceStore;

    public DatabaseResultController(
            DatabaseResultReferenceStore resultReferenceStore
    ) {
        this.resultReferenceStore =
                resultReferenceStore;
    }

    /*
     * resultReference만 신뢰하지 않는다.
     *
     * Store에서 현재 Spring Security 사용자와
     * 결과 생성 사용자가 동일한지 다시 검증한 뒤 반환한다.
     */
    @GetMapping("/{resultReference}")
    public ResponseEntity<DatabaseResultReference> getResult(
            @PathVariable String resultReference,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        DatabaseResultReference result =
                resultReferenceStore.getRequired(
                        resultReference,
                        currentUser
                );

        return ResponseEntity.ok(result);
    }
}