/*
 * =============================================================================
 * 클래스명 : DocumentRagContextBuilder
 * =============================================================================
 * 목적
 *  - 권한 검증이 완료된 문서 Chunk에서 Gemini 답변 생성에 필요한 텍스트만 추출한다.
 *  - organizationId, departmentId, securityLevel 등의 내부 Metadata를
 *    Gemini Context에 포함하지 않는다.
 *  - 여러 Chunk를 하나의 최소 RAG Context 문자열로 구성한다.
 */

package com.example.enterpriseai.service.document;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentRagContextBuilder {

    /*
     * 권한 검증이 완료된 Chunk 목록을
     * Gemini에 전달할 최소 Context로 구성한다.
     */
    public String build(
            List<Document> documents
    ) {

        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder context =
                new StringBuilder();

        int contextIndex = 1;

        for (Document document : documents) {

            if (document == null) {
                continue;
            }

            String text = document.getText();

            if (text == null || text.isBlank()) {
                continue;
            }

            // Gemini에는 권한 Metadata를 전달하지 않고
            // 검증된 Chunk의 실제 문서 내용만 전달한다.
            context.append("[문서 내용 ")
                    .append(contextIndex)
                    .append("]")
                    .append(System.lineSeparator())
                    .append(text.trim())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());

            contextIndex++;
        }

        return context.toString().trim();
    }
}