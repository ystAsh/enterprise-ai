/*
 * =============================================================================
 * 클래스명 : TemSystemDatabaseQueryExecutor
 * =============================================================================
 * 목적
 *  - enterprise-ai의 공통 DatabaseQueryExecutor와 별도 tem-system을 연결한다.
 *  - 서버에 등록된 Query Definition에 따라 tem-system의 기존 조회 API를 호출한다.
 *  - 자연어 질문을 직접 해석하지 않고 검증 완료된 실행 파라미터만 사용한다.
 *  - tem-system의 DB Schema, Repository, Service 구현을 직접 알지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameters;
import com.example.enterpriseai.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TemSystemDatabaseQueryExecutor
        implements DatabaseQueryExecutor {

    /*
     * 서버 내부 Query 식별자이다.
     *
     * LLM에 직접 노출하지 않고
     * Capability Registry를 통해 서버 내부에서 연결한다.
     */
    public static final String QUERY_SERIES_SEARCH =
            "TEM_SERIES_SEARCH";

    private static final String SOURCE =
            "TEM_SYSTEM";

    private final TemSystemClient temSystemClient;

    public TemSystemDatabaseQueryExecutor(
            TemSystemClient temSystemClient
    ) {
        this.temSystemClient = temSystemClient;
    }

    /*
     * 이 Adapter가 tem-system용 Query Definition인지 확인한다.
     *
     * source만으로 모든 Query를 실행하지 않고,
     * 실제 지원하는 queryKey도 함께 확인한다.
     */
    @Override
    public boolean supports(
            DatabaseQueryDefinition definition
    ) {

        if (definition == null) {
            return false;
        }

        return SOURCE.equals(definition.source())
                && QUERY_SERIES_SEARCH.equals(
                definition.queryKey()
        );
    }

    /*
     * 검증 완료된 실행 파라미터를 이용하여
     * tem-system의 기존 시리즈 검색 API를 호출한다.
     *
     * 반환값은 Raw Result이므로
     * 이후 DatabaseResultValidator 검증을 반드시 거쳐야 한다.
     */
    @Override
    public Map<String, Object> execute(
            DatabaseQueryDefinition definition,
            DatabaseQueryParameters parameters,
            CurrentUser currentUser
    ) {

        validateInput(
                definition,
                parameters,
                currentUser
        );

        String keyword =
                parameters.getRequiredString(
                        "keyword"
                );

        List<Map<String, Object>> items =
                temSystemClient.searchSeries(keyword)
                        .stream()
                        .map(result -> {

                            Map<String, Object> item =
                                    new LinkedHashMap<>();

                            item.put(
                                    "seriesCode",
                                    result.seriesCode()
                            );

                            item.put(
                                    "seriesName",
                                    result.seriesName()
                            );

                            item.put(
                                    "description",
                                    result.description()
                            );

                            return item;
                        })
                        .toList();

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

        return result;
    }

    /*
     * Adapter 호출 전에 최소 실행 조건을 다시 확인한다.
     */
    private void validateInput(
            DatabaseQueryDefinition definition,
            DatabaseQueryParameters parameters,
            CurrentUser currentUser
    ) {

        if (!supports(definition)) {
            throw new IllegalArgumentException(
                    "이 Executor에서 처리할 수 없는 Query입니다."
            );
        }

        if (parameters == null) {
            throw new IllegalArgumentException(
                    "Query 실행 파라미터가 없습니다."
            );
        }

        if (currentUser == null) {
            throw new SecurityException(
                    "인증된 사용자가 없습니다."
            );
        }
    }
}