/*
 * =============================================================================
 * 클래스명 : RestClientConfig
 * =============================================================================
 * 목적
 *  - enterprise-ai 내부에서 사용하는 RestClient.Builder Bean을 등록한다.
 *  - 기존 시스템 연동 Client가 공통 Builder를 주입받을 수 있도록 한다.
 */

package com.example.enterpriseai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}