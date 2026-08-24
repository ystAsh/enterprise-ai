/*
 * =============================================================================
 * 클래스명 : DatabaseQueryValidator
 * =============================================================================
 * 목적
 *  - Database RAG Query 실행 전에 공통 보안 정책을 검증한다.
 *  - 특정 회사, 업무, 테이블에 종속되지 않는다.
 *  - 허용되지 않은 Query 실행을 Java 계층에서 차단한다.
 */

package com.example.enterpriseai.service.security;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryExecutionPolicy;
import org.springframework.stereotype.Service;

@Service
public class DatabaseQueryValidator {

    /*
     * Query 실행 전 공통 보안 정책을 검증한다.
     *
     * 특정 업무 의미를 판단하지 않고,
     * 실행 허용 여부와 안전 범위만 검증한다.
     */
    public void validate(
            DatabaseQueryDefinition definition,
            DatabaseQueryExecutionPolicy policy
    ) {

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Query 정의가 없습니다."
            );
        }

        if (policy == null) {
            throw new IllegalArgumentException(
                    "Query 실행 정책이 없습니다."
            );
        }

        validateQueryKey(
                definition.queryKey(),
                policy
        );

        validateExecutionType(
                definition.executionType(),
                policy
        );
    }

    /*
     * 서버에서 허용한 Query인지 검증한다.
     */
    private void validateQueryKey(
            String queryKey,
            DatabaseQueryExecutionPolicy policy
    ) {

        if (!policy.allowedQueryKeys().isEmpty()
                && !policy.allowedQueryKeys().contains(queryKey)) {

            throw new SecurityException(
                    "허용되지 않은 Query입니다."
            );
        }
    }

    /*
     * 허용된 실행 방식인지 검증한다.
     */
    private void validateExecutionType(
            String executionType,
            DatabaseQueryExecutionPolicy policy
    ) {

        if (!policy.allowedExecutionTypes().isEmpty()
                && !policy.allowedExecutionTypes().contains(executionType)) {

            throw new SecurityException(
                    "허용되지 않은 Query 실행 방식입니다."
            );
        }
    }
}