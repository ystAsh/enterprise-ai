/*
 * =============================================================================
 * 클래스명 : DocumentChunkService
 * =============================================================================
 * 목적
 *  - Parser에서 추출한 문서 텍스트를 검색용 Chunk로 분할한다.
 *  - 현재 단계에서는 Chunk 분할만 담당하고 Embedding이나 VectorStore 저장은 하지 않는다.
 */

package com.example.enterpriseai.service.document;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentChunkService {

    private final TokenTextSplitter textSplitter;

    public DocumentChunkService() {

        // Spring AI 2.x 권장 방식으로 TokenTextSplitter를 생성한다.
        this.textSplitter =
                TokenTextSplitter.builder()
                        .withChunkSize(500)
                        .build();
    }

    // 전체 문서 텍스트를 여러 Chunk 문자열로 분할한다.
    public List<String> split(String text) {

        validateText(text);

        Document sourceDocument =
                new Document(text);

        return textSplitter
                .apply(List.of(sourceDocument))
                .stream()
                .map(Document::getText)
                .toList();
    }

    private void validateText(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Chunk로 분할할 문서 텍스트가 없습니다."
            );
        }
    }
}