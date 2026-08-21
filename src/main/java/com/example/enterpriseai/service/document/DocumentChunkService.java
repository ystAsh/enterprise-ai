/*
 * =============================================================================
 * 클래스명 : DocumentChunkService
 * =============================================================================
 * 목적
 *  - Parser에서 추출한 문서 전체 텍스트를 검색용 Chunk로 분할한다.
 *  - 각 Chunk에 Document RAG 권한 검색에 필요한 Metadata를 추가한다.
 *  - 아직 Embedding이나 VectorStore 저장은 수행하지 않는다.
 */

package com.example.enterpriseai.service.document;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentChunkService {

    private final TokenTextSplitter textSplitter;

    public DocumentChunkService() {

        this.textSplitter =
                TokenTextSplitter.builder()
                        .withChunkSize(500)
                        .build();
    }

    /*
     * 전체 문서 텍스트를 Chunk로 분할하고
     * 각 Chunk에 검색/권한 검증용 Metadata를 추가한다.
     */
    public List<Document> split(
            String text,
            Long documentId,
            Long organizationId,
            Long departmentId,
            int securityLevel,
            String fileName
    ) {

        validateText(text);

        Document sourceDocument =
                new Document(text);

        List<Document> splitDocuments =
                textSplitter.apply(
                        List.of(sourceDocument)
                );

        List<Document> chunks =
                new ArrayList<>();

        for (int i = 0; i < splitDocuments.size(); i++) {

            Document splitDocument =
                    splitDocuments.get(i);

            Map<String, Object> metadata =
                    new HashMap<>();

            metadata.put(
                    "documentId",
                    documentId
            );

            metadata.put(
                    "chunkIndex",
                    i
            );

            metadata.put(
                    "organizationId",
                    organizationId
            );

            metadata.put(
                    "departmentId",
                    departmentId
            );

            metadata.put(
                    "securityLevel",
                    securityLevel
            );

            metadata.put(
                    "fileName",
                    fileName
            );

            Document chunk =
                    new Document(
                            splitDocument.getText(),
                            metadata
                    );

            chunks.add(chunk);
        }

        return chunks;
    }

    // Chunking 전에 Parser 결과를 검증한다.
    private void validateText(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Chunk로 분할할 문서 텍스트가 없습니다."
            );
        }
    }
}