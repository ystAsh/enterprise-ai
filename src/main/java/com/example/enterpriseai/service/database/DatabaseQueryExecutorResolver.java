/*
 * =============================================================================
 * 클래스명 : DatabaseQueryExecutorResolver
 * =============================================================================
 * 목적
 *  - 검증된 DatabaseQueryDefinition을 실행할 수 있는 Executor를 선택한다.
 *  - 특정 회사, 업무, 테이블, Repository, Mapper에 종속되지 않는다.
 *  - 실행 가능한 Executor가 없거나 여러 개이면 안전하게 실행을 차단한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseQueryExecutorResolver {

    private final List<DatabaseQueryExecutor> executors;

    public DatabaseQueryExecutorResolver(
            List<DatabaseQueryExecutor> executors
    ) {
        this.executors = List.copyOf(executors);
    }

    /*
     * Query Definition을 처리할 수 있는 Executor 하나를 선택한다.
     *
     * 0개:
     *  - 실행 가능한 Query가 아니므로 차단한다.
     *
     * 2개 이상:
     *  - 실행 경로가 모호하므로 차단한다.
     *
     * 반드시 정확히 하나의 Executor만 선택되어야 한다.
     */
    public DatabaseQueryExecutor resolve(
            DatabaseQueryDefinition definition
    ) {

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Query 정의가 없습니다."
            );
        }

        List<DatabaseQueryExecutor> matchedExecutors =
                executors.stream()
                        .filter(executor ->
                                executor.supports(definition)
                        )
                        .toList();

        if (matchedExecutors.isEmpty()) {
            throw new IllegalStateException(
                    "실행 가능한 Database Query Executor가 없습니다."
            );
        }

        if (matchedExecutors.size() > 1) {
            throw new IllegalStateException(
                    "Database Query 실행 경로가 중복되었습니다."
            );
        }

        return matchedExecutors.getFirst();
    }
}