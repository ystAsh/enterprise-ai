/*
 * =============================================================================
 * 클래스명 : DocumentEmbeddingService
 * =============================================================================
 * 목적
 *  - 검증 완료된 문서 Chunk를 Spring AI VectorStore에 저장한다.
 *  - Embedding 생성과 PGVector 저장은 VectorStore 구현체에 위임한다.
 *  - 구체적인 PgVectorStore가 아니라 VectorStore 인터페이스에 의존한다.
 */

package com.example.enterpriseai.service.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentEmbeddingService {

    private final VectorStore vectorStore;

    public DocumentEmbeddingService(
            VectorStore vectorStore
    ) {
        this.vectorStore = vectorStore;
    }

    /*
     * 검증 완료된 Chunk를 Embedding하고 VectorStore에 저장한다.
     */
    public void store(
            List<Document> documents
    ) {

        validateDocuments(documents);

        vectorStore.add(documents);
    }

    // VectorStore 저장 전 최소 입력값을 검증한다.
    private void validateDocuments(
            List<Document> documents
    ) {

        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException(
                    "저장할 문서 Chunk가 없습니다."
            );
        }

        for (Document document : documents) {

            if (document == null) {
                throw new IllegalArgumentException(
                        "문서 Chunk에 null 값이 포함되어 있습니다."
                );
            }

            if (document.getText() == null
                    || document.getText().isBlank()) {

                throw new IllegalArgumentException(
                        "내용이 없는 문서 Chunk는 저장할 수 없습니다."
                );
            }
        }
    }
}