/*
 * =============================================================================
 * 클래스명 : DocumentSearchResultValidator
 * =============================================================================
 * 목적
 *  - Vector Search가 반환한 문서 Chunk의 권한 정보를 다시 검증한다.
 *  - 권한 검증에 실패한 Chunk가 RAG Context나 Gemini로 전달되지 않도록 차단한다.
 */

package com.example.enterpriseai.service.security;

import com.example.enterpriseai.security.CurrentUser;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DocumentSearchResultValidator {

    /*
     * Vector Search 결과 전체를 검증한다.
     *
     * 하나의 Chunk라도 권한 검증에 실패하면
     * 검색 결과 전체를 사용하지 않는다.
     */
    public List<Document> validate(
            List<Document> documents,
            CurrentUser currentUser
    ) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        for (Document document : documents) {
            validateDocument(
                    document,
                    currentUser
            );
        }

        return documents;
    }

    /*
     * 하나의 Chunk에 저장된 Metadata와
     * 현재 로그인 사용자의 권한을 비교한다.
     */
    private void validateDocument(
            Document document,
            CurrentUser currentUser
    ) {

        if (document == null) {
            throw new IllegalStateException(
                    "Vector Search 결과에 null 문서가 포함되어 있습니다."
            );
        }

        Map<String, Object> metadata =
                document.getMetadata();

        if (metadata == null || metadata.isEmpty()) {
            throw new IllegalStateException(
                    "Vector Search 결과에 권한 Metadata가 없습니다."
            );
        }

        long organizationId =
                getRequiredLong(
                        metadata,
                        "organizationId"
                );

        long departmentId =
                getRequiredLong(
                        metadata,
                        "departmentId"
                );

        int securityLevel =
                getRequiredInt(
                        metadata,
                        "securityLevel"
                );

        // 다른 조직의 Chunk는 사용할 수 없다.
        if (organizationId
                != currentUser.getOrganizationId()) {

            throw new IllegalStateException(
                    "권한 검증에 실패한 문서가 검색되었습니다."
            );
        }

        // 다른 부서의 Chunk는 사용할 수 없다.
        if (departmentId
                != currentUser.getDepartmentId()) {

            throw new IllegalStateException(
                    "권한 검증에 실패한 문서가 검색되었습니다."
            );
        }

        // 사용자보다 높은 보안등급의 Chunk는 사용할 수 없다.
        if (securityLevel
                > currentUser.getSecurityLevel()) {

            throw new IllegalStateException(
                    "권한 검증에 실패한 문서가 검색되었습니다."
            );
        }
    }

    /*
     * Metadata의 필수 숫자 값을 long으로 변환한다.
     */
    private long getRequiredLong(
            Map<String, Object> metadata,
            String key
    ) {

        Object value = metadata.get(key);

        if (!(value instanceof Number number)) {
            throw new IllegalStateException(
                    "필수 Metadata가 없거나 숫자가 아닙니다: " + key
            );
        }

        return number.longValue();
    }

    /*
     * Metadata의 필수 숫자 값을 int로 변환한다.
     */
    private int getRequiredInt(
            Map<String, Object> metadata,
            String key
    ) {

        Object value = metadata.get(key);

        if (!(value instanceof Number number)) {
            throw new IllegalStateException(
                    "필수 Metadata가 없거나 숫자가 아닙니다: " + key
            );
        }

        return number.intValue();
    }
}