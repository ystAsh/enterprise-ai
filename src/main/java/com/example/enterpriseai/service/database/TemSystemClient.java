/*
 * =============================================================================
 * 클래스명 : TemSystemClient
 * =============================================================================
 * 목적
 *  - enterprise-ai에서 tem-system의 내부 검증 조회 API를 호출한다.
 *  - tem-system의 DB에 직접 접근하지 않는다.
 *  - 기존 시스템의 응답 필드 의미를 AI 공통 계층에서 하드코딩하지 않는다.
 *  - 기존 시스템이 반환한 결과를 범용 Map 구조로 전달한다.
 */

package com.example.enterpriseai.service.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class TemSystemClient {

    private final RestClient restClient;

    public TemSystemClient(
            RestClient.Builder restClientBuilder,
            @Value("${tem-system.base-url:http://localhost:8081}")
            String baseUrl
    ) {
        this.restClient =
                restClientBuilder
                        .baseUrl(baseUrl)
                        .build();
    }

    /*
     * 현재 tem-system에 존재하는 검증된 검색 API를 호출한다.
     *
     * 응답의 업무 필드명과 의미는 여기서 해석하지 않는다.
     * 반환 결과는 이후 DatabaseResultValidator 검증 전까지 신뢰하지 않는다.
     */
    public List<Map<String, Object>> searchSeries(
            String keyword
    ) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                    "검색 조건이 없습니다."
            );
        }

        List<Map<String, Object>> result =
                restClient.get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/internal/series/search")
                                        .queryParam(
                                                "keyword",
                                                keyword.trim()
                                        )
                                        .build()
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<>() {
                                }
                        );

        return result == null
                ? List.of()
                : List.copyOf(result);
    }
}