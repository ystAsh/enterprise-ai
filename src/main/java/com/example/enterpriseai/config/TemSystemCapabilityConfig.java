/*
 * =============================================================================
 * 클래스명 : TemSystemCapabilityConfig
 * =============================================================================
 * 목적
 *  - tem-system에서 AI 질의 경로로 사용할 Capability와 Query Definition을 등록한다.
 *  - 공통 Database RAG 계층을 수정하지 않고 기존 시스템별 조회 기능을 연결한다.
 *  - 실제 queryKey와 실행/검증 정책은 서버 내부에서만 관리한다.
 */

package com.example.enterpriseai.config;

import com.example.enterpriseai.dto.DatabaseQueryCapability;
import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryExecutionPolicy;
import com.example.enterpriseai.dto.DatabaseValidationPolicy;
import com.example.enterpriseai.service.database.DatabaseQueryCapabilityRegistry;
import com.example.enterpriseai.service.database.TemSystemDatabaseQueryExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class TemSystemCapabilityConfig {

    private static final String QUERY_KEY =
            TemSystemDatabaseQueryExecutor.QUERY_SERIES_SEARCH;

    private static final String EXECUTION_TYPE =
            "HTTP_ADAPTER";

    @Bean
    public DatabaseQueryCapabilityRegistry.RegisteredCapability
    temSystemSearchCapability() {

        DatabaseQueryCapability capability =
                new DatabaseQueryCapability(
                        "TEM_SYSTEM_SEARCH_CAPABILITY",
                        "등록된 업무 데이터를 조건에 따라 조회한다.",
                        Set.of(
                                "SEARCH",
                                "LOOKUP"
                        )
                );

        return new DatabaseQueryCapabilityRegistry.RegisteredCapability(
                capability,
                QUERY_KEY
        );
    }

    @Bean
    public DatabaseQueryDefinition
    temSystemSearchDefinition() {

        DatabaseQueryExecutionPolicy executionPolicy =
                new DatabaseQueryExecutionPolicy(
                        Set.of(QUERY_KEY),
                        Set.of(EXECUTION_TYPE),
                        100,
                        5000,
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
                        10,
                        100,
                        1000,
                        3,
                        false,
                        true
                );

        return new DatabaseQueryDefinition(
                "LOOKUP",
                QUERY_KEY,
                "등록된 업무 데이터 조회",
                "tem-system",
                EXECUTION_TYPE,
                Set.of("keyword"),
                executionPolicy,
                validationPolicy
        );
    }
}