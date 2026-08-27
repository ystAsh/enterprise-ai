/*
 * =============================================================================
 * 클래스명 : TemSystemClient
 * =============================================================================
 * 목적
 *  - enterprise-ai에서 tem-system의 내부 검증 조회 API를 호출한다.
 *  - tem-system의 DB에 직접 접근하지 않는다.
 *  - 기존 시스템이 반환한 최소 조회 결과만 전달한다.
 */

package com.example.enterpriseai.service.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class TemSystemClient {

    private final RestClient restClient;

    public TemSystemClient(
            RestClient.Builder restClientBuilder,
            @Value("${tem-system.base-url:http://localhost:8081}")
            String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    /*
     * tem-system의 기존 시리즈 검색 기능을 호출한다.
     */
    public List<SeriesSearchResponse> searchSeries(
            String keyword
    ) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                    "검색 조건이 없습니다."
            );
        }

        List<SeriesSearchResponse> result =
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

    /*
     * tem-system 내부 API가 제공하는 최소 응답 구조와 대응한다.
     */
    public record SeriesSearchResponse(
            String seriesCode,
            String seriesName,
            String description
    ) {
    }
}