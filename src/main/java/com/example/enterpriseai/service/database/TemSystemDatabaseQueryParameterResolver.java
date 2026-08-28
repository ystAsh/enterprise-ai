/*
 * =============================================================================
 * 클래스명 : TemSystemDatabaseQueryParameterResolver
 * =============================================================================
 * 목적
 *  - tem-system에 등록된 Query Definition의 실행 파라미터 후보를 생성한다.
 *  - 사용자 자연어 질문에서 기존 시스템 조회에 필요한 값을 추출한다.
 *  - 생성된 값은 검증 전 Candidate이며 직접 Query 실행에 사용하지 않는다.
 *  - tem-system 업무 차이를 공통 Database RAG 계층으로 확산시키지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemSystemDatabaseQueryParameterResolver
        implements DatabaseQueryParameterResolver {

    @Override
    public boolean supports(
            DatabaseQueryDefinition definition
    ) {

        if (definition == null) {
            return false;
        }

        return TemSystemDatabaseQueryExecutor.QUERY_SERIES_SEARCH.equals(
                definition.queryKey()
        );
    }

    @Override
    public DatabaseQueryParameterCandidate resolve(
            String question,
            DatabaseQueryDefinition definition
    ) {

        if (!supports(definition)) {
            throw new IllegalArgumentException(
                    "지원하지 않는 Query Definition입니다."
            );
        }

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }

        String keyword =
                extractKeyword(question);

        return new DatabaseQueryParameterCandidate(
                Map.of(
                        "keyword",
                        keyword
                )
        );
    }

    /*
     * 현재 학습용 tem-system 조회에 필요한 검색어 후보를 추출한다.
     *
     * 추출된 값은 신뢰하지 않으며 이후
     * DatabaseQueryParameterValidator 검증을 반드시 거친다.
     */
    private String extractKeyword(
            String question
    ) {

        String normalized =
                question.trim();

        String[] tokens =
                normalized.split("\\s+");

        for (String token : tokens) {

            String candidate =
                    token.replaceAll(
                            "[^A-Za-z0-9_-]",
                            ""
                    );

            if (!candidate.isBlank()) {
                return candidate;
            }
        }

        throw new IllegalArgumentException(
                "Query 파라미터 후보를 추출할 수 없습니다."
        );
    }
}