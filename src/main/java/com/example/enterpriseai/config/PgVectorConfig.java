/*
 * =============================================================================
 * 클래스명 : PgVectorConfig
 * =============================================================================
 * 목적
 *  - PostgreSQL 전용 JdbcTemplate과 EmbeddingModel을 사용하여 PgVectorStore를 구성한다.
 *  - 서비스 계층에서는 PgVectorStore 구현체가 아니라 VectorStore 인터페이스를 사용하게 한다.
 */

package com.example.enterpriseai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration(proxyBeanMethods = false)
public class PgVectorConfig {

    @Bean
    public VectorStore vectorStore(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
            EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(vectorJdbcTemplate, embeddingModel)
                .dimensions(768)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .build();
    }
}