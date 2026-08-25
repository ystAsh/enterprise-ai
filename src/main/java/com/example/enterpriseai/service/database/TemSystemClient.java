/*
 * =============================================================================
 * 클래스명 : TemSystemClient
 * =============================================================================
 * 목적
 *  - enterprise-ai에서 별도 tem-system의 내부 조회 API를 호출한다.
 *  - tem-system의 실제 DB Schema, Repository, Service 구현을 알지 않는다.
 *  - 기존 시스템이 제공하는 검증된 조회 API의 결과만 받아온다.
 */

package com.example.enterpriseai.service.database;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class TemSystemClient {

    private final RestClient restClient;

    public TemSystemClient(
            RestClient.Builder restClientBuilder
    ) {
        this.restClient =
                restClientBuilder
                        .baseUrl("http://localhost:8081")
                        .build();
    }

    /*
     * tem-system의 기존 시리즈 검색 기능을 호출한다.
     *
     * enterprise-ai는 tem-system의 DB나 Repository를 직접 조회하지 않는다.
     */
    public List<SeriesSearchResult> searchSeries(
            String keyword
    ) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                    "시리즈 검색어가 없습니다."
            );
        }

        List<SeriesSearchResult> result =
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
                                new org.springframework.core.ParameterizedTypeReference<
                                        List<SeriesSearchResult>>() {
                                }
                        );

        if (result == null) {
            throw new IllegalStateException(
                    "기존 시스템 조회 결과가 없습니다."
            );
        }

        return List.copyOf(result);
    }

    /*
     * tem-system에서 반환하는 최소 시리즈 조회 결과이다.
     *
     * 내부 ID, SQL, Repository 정보는 포함하지 않는다.
     */
    public record SeriesSearchResult(
            String seriesCode,
            String seriesName,
            String description
    ) {
    }
}