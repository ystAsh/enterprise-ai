/*
 * =============================================================================
 * 클래스명 : TemSystemCapabilityConfig
 * =============================================================================
 * 목적
 *  - tem-system의 기존 조회 기능을 자연어 질문 매칭용 Capability로 등록한다.
 *  - LLM에는 안전한 Capability 정보만 제공하고 실제 queryKey 연결은
 *    서버 내부 Registry에서만 사용한다.
 *  - tem-system 연동 전용 설정이며 공통 AI 계층에 업무 로직을 넣지 않는다.
 */

package com.example.enterpriseai.config;

import com.example.enterpriseai.dto.DatabaseQueryCapability;
import com.example.enterpriseai.service.database.DatabaseQueryCapabilityRegistry;
import com.example.enterpriseai.service.database.TemSystemDatabaseQueryExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class TemSystemCapabilityConfig {

    /*
     * tem-system의 기존 시리즈 검색 기능을
     * 자연어 질문과 매칭할 수 있는 안전한 Capability로 등록한다.
     */
    @Bean
    public DatabaseQueryCapabilityRegistry.RegisteredCapability
    temSeriesSearchCapability() {

        DatabaseQueryCapability capability =
                new DatabaseQueryCapability(
                        "TEM_SERIES_SEARCH_CAPABILITY",
                        "시리즈 코드 또는 이름 조건으로 시리즈 정보를 검색한다.",
                        Set.of(
                                "SEARCH"
                        )
                );

        return new DatabaseQueryCapabilityRegistry.RegisteredCapability(
                capability,
                TemSystemDatabaseQueryExecutor.QUERY_SERIES_SEARCH
        );
    }
}