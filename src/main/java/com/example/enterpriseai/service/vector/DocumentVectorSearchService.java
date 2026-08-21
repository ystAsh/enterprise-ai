/*
 * =============================================================================
 * 클래스명 : DocumentVectorSearchService
 * =============================================================================
 * 목적
 *  - 로그인 사용자의 권한 범위를 강제하여 PGVector 문서 Chunk를 검색한다.
 *  - organizationId, departmentId, securityLevel 조건을 Java 서버에서 생성한다.
 *  - Vector Search 결과를 다시 권한 검증한 후 반환한다.
 *  - 구체적인 PgVectorStore가 아니라 VectorStore 인터페이스에 의존한다.
 */

package com.example.enterpriseai.service.vector;

import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.security.DocumentSearchResultValidator;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentVectorSearchService {

    private static final int DEFAULT_TOP_K = 5;

    private final VectorStore vectorStore;
    private final DocumentSearchResultValidator resultValidator;

    public DocumentVectorSearchService(
            VectorStore vectorStore,
            DocumentSearchResultValidator resultValidator
    ) {
        this.vectorStore = vectorStore;
        this.resultValidator = resultValidator;
    }

    /*
     * 로그인 사용자의 권한 범위 안에서
     * 질문과 의미적으로 유사한 문서 Chunk를 검색한다.
     *
     * 검색 전:
     *  - organizationId
     *  - departmentId
     *  - securityLevel
     * 조건을 Metadata Filter로 강제한다.
     *
     * 검색 후:
     *  - 반환된 Chunk의 권한 Metadata를 다시 검증한다.
     */
    public List<Document> search(
            String query,
            CurrentUser currentUser
    ) {

        validateQuery(query);

        // 권한 Filter는 클라이언트나 LLM이 아니라
        // Spring Security의 CurrentUser를 기준으로 서버에서 생성한다.
        FilterExpressionBuilder builder =
                new FilterExpressionBuilder();

        var filterExpression =
                builder.and(
                        builder.eq(
                                "organizationId",
                                currentUser.getOrganizationId()
                        ),
                        builder.and(
                                builder.eq(
                                        "departmentId",
                                        currentUser.getDepartmentId()
                                ),
                                builder.lte(
                                        "securityLevel",
                                        currentUser.getSecurityLevel()
                                )
                        )
                ).build();

        SearchRequest searchRequest =
                SearchRequest.builder()
                        .query(query)
                        .topK(DEFAULT_TOP_K)
                        .filterExpression(filterExpression)
                        .build();

        // 권한 Filter가 적용된 상태에서 Vector Search를 수행한다.
        List<Document> documents =
                vectorStore.similaritySearch(
                        searchRequest
                );

        // 검색 결과를 바로 사용하지 않고
        // 현재 로그인 사용자의 권한 범위와 다시 비교한다.
        return resultValidator.validate(
                documents,
                currentUser
        );
    }

    /*
     * Vector Search 전에 질문 문자열을 최소 검증한다.
     */
    private void validateQuery(
            String query
    ) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "검색 질문이 없습니다."
            );
        }
    }
}