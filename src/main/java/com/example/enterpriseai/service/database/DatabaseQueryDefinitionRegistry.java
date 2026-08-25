/*
 * =============================================================================
 * 클래스명 : DatabaseQueryDefinitionRegistry
 * =============================================================================
 * 목적
 *  - 서버에 등록된 DatabaseQueryDefinition만 조회할 수 있도록 관리한다.
 *  - 클라이언트나 LLM이 임의의 queryKey / executionType / 검증 정책을
 *    직접 만들어 실행하는 것을 차단한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는 공통 Registry 역할을 한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseQueryDefinitionRegistry {

    private final Map<String, DatabaseQueryDefinition> definitions;

    public DatabaseQueryDefinitionRegistry(
            List<DatabaseQueryDefinition> definitionList
    ) {

        Map<String, DatabaseQueryDefinition> registry =
                new HashMap<>();

        for (DatabaseQueryDefinition definition : definitionList) {

            if (definition == null) {
                throw new IllegalStateException(
                        "등록할 Query Definition이 null입니다."
                );
            }

            String queryKey =
                    definition.queryKey();

            if (registry.containsKey(queryKey)) {
                throw new IllegalStateException(
                        "중복된 Query Definition이 등록되었습니다."
                );
            }

            registry.put(
                    queryKey,
                    definition
            );
        }

        this.definitions =
                Collections.unmodifiableMap(registry);
    }

    /*
     * 서버에 등록된 Query Definition을 queryKey로 조회한다.
     *
     * 미등록 queryKey는 실행 경로로 전달하지 않고 즉시 차단한다.
     */
    public DatabaseQueryDefinition getRequired(
            String queryKey
    ) {

        if (queryKey == null || queryKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 식별자가 없습니다."
            );
        }

        DatabaseQueryDefinition definition =
                definitions.get(queryKey);

        if (definition == null) {
            throw new SecurityException(
                    "등록되지 않은 Query입니다."
            );
        }

        return definition;
    }

    /*
     * 현재 서버에 등록된 Query Definition 개수를 반환한다.
     * 내부 상태 확인용이며 Definition 전체를 외부에 노출하지 않는다.
     */
    public int size() {
        return definitions.size();
    }
}