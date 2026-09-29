/*
 * =============================================================================
 * 클래스명 : SafeQueryJdbcConfig
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL 전용 JdbcClient를 MSSQL Primary DataSource에 연결한다.
 *  - PostgreSQL Vector DataSource와 Safe Query 실행 경로를 명확하게 분리한다.
 *  - Safe Query에만 전용 Query Timeout을 적용하여 장시간 실행을 제한한다.
 */

package com.example.enterpriseai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class SafeQueryJdbcConfig {

    private static final int SAFE_QUERY_TIMEOUT_SECONDS = 3;

    @Bean(name = "safeQueryJdbcClient")
    public JdbcClient safeQueryJdbcClient(
            DataSource dataSource
    ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Safe Text-to-SQL 실행에만 Statement Query Timeout을 강제한다.
        jdbcTemplate.setQueryTimeout(SAFE_QUERY_TIMEOUT_SECONDS);

        return JdbcClient.create(jdbcTemplate);
    }
}