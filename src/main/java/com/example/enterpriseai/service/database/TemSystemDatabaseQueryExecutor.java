/*
 * =============================================================================
 * 클래스명 : TemSystemDatabaseQueryExecutor
 * =============================================================================
 * 목적
 *  - enterprise-ai의 공통 DatabaseQueryExecutor와 tem-system 기존 조회 API를 연결한다.
 *  - 검증 완료된 Query 파라미터만 기존 시스템에 전달한다.
 *  - 기존 시스템의 업무 필드 의미를 해석하거나 자연어 답변을 만들지 않는다.
 *  - Raw Result와 실제 반환 건수를 공통 실행 결과 형태로 반환한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryExecutionResult;
import com.example.enterpriseai.dto.DatabaseQueryParameters;
import com.example.enterpriseai.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemSystemDatabaseQueryExecutor
        implements DatabaseQueryExecutor {

    public static final String QUERY_SERIES_SEARCH =
            "TEM_SERIES_SEARCH";

    private final TemSystemClient temSystemClient;

    public TemSystemDatabaseQueryExecutor(
            TemSystemClient temSystemClient
    ) {
        this.temSystemClient =
                temSystemClient;
    }

    @Override
    public boolean supports(
            DatabaseQueryDefinition definition
    ) {

        if (definition == null) {
            return false;
        }

        return QUERY_SERIES_SEARCH.equals(
                definition.queryKey()
        );
    }

    @Override
    public DatabaseQueryExecutionResult execute(
            DatabaseQueryDefinition definition,
            DatabaseQueryParameters parameters,
            CurrentUser currentUser
    ) {

        if (!supports(definition)) {
            throw new IllegalArgumentException(
                    "지원하지 않는 Query Definition입니다."
            );
        }

        if (parameters == null) {
            throw new IllegalArgumentException(
                    "검증된 Query 파라미터가 없습니다."
            );
        }

        if (currentUser == null) {
            throw new SecurityException(
                    "인증된 사용자 정보가 없습니다."
            );
        }

        String keyword =
                parameters.getRequiredString(
                        "keyword"
                );

        List<Map<String, Object>> items =
                temSystemClient.searchSeries(
                        keyword
                );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "items",
                items
        );

        result.put(
                "count",
                items.size()
        );

        return DatabaseQueryExecutionResult.returned(
                result,
                items.size()
        );
    }
}