/*
 * =============================================================================
 * 클래스명 : DatabaseQueryParameterResolverSelector
 * =============================================================================
 * 목적
 *  - Query Definition을 처리할 수 있는 DatabaseQueryParameterResolver를 선택한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 *  - 처리 가능한 Resolver가 없거나 여러 개이면 안전하게 차단한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseQueryParameterResolverSelector {

    private final List<DatabaseQueryParameterResolver> resolvers;

    public DatabaseQueryParameterResolverSelector(
            List<DatabaseQueryParameterResolver> resolvers
    ) {
        this.resolvers =
                List.copyOf(resolvers);
    }

    public DatabaseQueryParameterResolver resolve(
            DatabaseQueryDefinition definition
    ) {

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Query 정의가 없습니다."
            );
        }

        List<DatabaseQueryParameterResolver> matchedResolvers =
                resolvers.stream()
                        .filter(resolver ->
                                resolver.supports(definition)
                        )
                        .toList();

        if (matchedResolvers.isEmpty()) {
            throw new IllegalStateException(
                    "사용 가능한 Query Parameter Resolver가 없습니다."
            );
        }

        if (matchedResolvers.size() > 1) {
            throw new IllegalStateException(
                    "Query Parameter Resolver가 중복되었습니다."
            );
        }

        return matchedResolvers.getFirst();
    }
}