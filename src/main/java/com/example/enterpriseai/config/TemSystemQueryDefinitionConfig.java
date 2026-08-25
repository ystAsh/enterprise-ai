/*
 * =============================================================================
 * 클래스명 : TemSystemQueryDefinitionConfig
 * =============================================================================
 * 목적
 *  - tem-system 연동에 사용할 서버 허용 Query Definition을 등록한다.
 *  - LLM이나 클라이언트가 Query 실행 정보와 검증 정책을 결정하지 못하게 한다.
 *  - 기존 시스템 Adapter와 공통 Database RAG 실행 계층을 연결한다.
 */

package com.example.enterpriseai.config;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryExecutionPolicy;
import com.example.enterpriseai.dto.DatabaseValidationPolicy;
import com.example.enterpriseai.service.database.TemSystemDatabaseQueryExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class TemSystemQueryDefinitionConfig {

    /*
     * tem-system의 기존 시리즈 검색 기능을
     * AI 질의 경로에서 사용할 수 있도록 서버 allowlist에 등록한다.
     */
    @Bean
    public DatabaseQueryDefinition temSeriesSearchQueryDefinition() {

        String queryKey =
                TemSystemDatabaseQueryExecutor.QUERY_SERIES_SEARCH;

        DatabaseQueryExecutionPolicy executionPolicy =
                new DatabaseQueryExecutionPolicy(
                        Set.of(queryKey),
                        Set.of("INTERNAL_API"),
                        20,
                        3000,
                        true
                );

        DatabaseValidationPolicy validationPolicy =
                new DatabaseValidationPolicy(
                        Set.of(
                                "items",
                                "count",
                                "seriesCode",
                                "seriesName",
                                "description"
                        ),
                        Set.of(),
                        5,
                        20,
                        500,
                        3,
                        true,
                        true
                );

        return new DatabaseQueryDefinition(
                "SERIES_SEARCH",
                queryKey,
                "시리즈 검색",
                "TEM_SYSTEM",
                "INTERNAL_API",
                executionPolicy,
                validationPolicy
        );
    }
}