/*
 * =============================================================================
 * 클래스명 : VectorDataSourceConfig
 * =============================================================================
 * 목적
 *  - PostgreSQL + PGVector 전용 DataSource와 JdbcTemplate을 구성한다.
 *  - MSSQL Primary DataSource와 Vector Database 연결을 명확하게 분리한다.
 */

package com.example.enterpriseai.config;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class VectorDataSourceConfig {

    @Bean(name = "vectorDataSourceProperties", defaultCandidate = false)
    @ConfigurationProperties("app.datasource.vector")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "vectorDataSource", defaultCandidate = false)
    public HikariDataSource vectorDataSource(
            @Qualifier("vectorDataSourceProperties")
            DataSourceProperties properties) {

        return properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "vectorJdbcTemplate", defaultCandidate = false)
    public JdbcTemplate vectorJdbcTemplate(
            @Qualifier("vectorDataSource")
            HikariDataSource vectorDataSource) {

        return new JdbcTemplate(vectorDataSource);
    }
}