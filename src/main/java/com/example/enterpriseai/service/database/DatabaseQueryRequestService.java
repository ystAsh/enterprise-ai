/*
 * =============================================================================
 * 클래스명 : DatabaseQueryRequestService
 * =============================================================================
 * 목적
 *  - 자연어 Database 질문을 서버의 검증된 Query 실행 흐름에 연결한다.
 *  - 등록된 Capability/Query를 항상 우선 사용한다.
 *  - 등록된 Query로 처리할 수 없는 경우에만 Safe Text-to-SQL로 fallback한다.
 *  - 권한/검증/실행 실패를 Safe Text-to-SQL로 우회하지 않는다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;
import com.example.enterpriseai.dto.DatabaseQueryParameters;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DatabaseQueryRequestService {

    private final DatabaseQueryCapabilityResolver capabilityResolver;
    private final DatabaseQueryDefinitionRegistry definitionRegistry;
    private final DatabaseQueryParameterResolverSelector parameterResolverSelector;
    private final DatabaseQueryParameterValidator parameterValidator;
    private final DatabaseQueryExecutionService executionService;
    private final DatabaseSafeTextToSqlService safeTextToSqlService;

    public DatabaseQueryRequestService(
            DatabaseQueryCapabilityResolver capabilityResolver,
            DatabaseQueryDefinitionRegistry definitionRegistry,
            DatabaseQueryParameterResolverSelector parameterResolverSelector,
            DatabaseQueryParameterValidator parameterValidator,
            DatabaseQueryExecutionService executionService,
            DatabaseSafeTextToSqlService safeTextToSqlService
    ) {
        this.capabilityResolver = capabilityResolver;
        this.definitionRegistry = definitionRegistry;
        this.parameterResolverSelector = parameterResolverSelector;
        this.parameterValidator = parameterValidator;
        this.executionService = executionService;
        this.safeTextToSqlService = safeTextToSqlService;
    }

    /*
     * 등록된 Database Query를 우선 사용하고,
     * 처리 가능한 Capability가 없을 때만 Safe Text-to-SQL로 fallback한다.
     */
    public DatabaseQueryResult execute(
            String question,
            CurrentUser currentUser
    ) {
        validateInput(question, currentUser);

        Optional<String> queryKey =
                capabilityResolver.tryResolveQueryKey(
                        question
                );

        if (queryKey.isPresent()) {
            return executeRegisteredQuery(
                    question,
                    queryKey.get(),
                    currentUser
            );
        }

        return safeTextToSqlService.execute(
                question,
                currentUser
        );
    }

    // 기존 Phase 10 등록 Query 실행 흐름은 그대로 유지한다.
    private DatabaseQueryResult executeRegisteredQuery(
            String question,
            String queryKey,
            CurrentUser currentUser
    ) {
        DatabaseQueryDefinition definition =
                definitionRegistry.getRequired(
                        queryKey
                );

        DatabaseQueryParameterResolver parameterResolver =
                parameterResolverSelector.resolve(
                        definition
                );

        DatabaseQueryParameterCandidate candidate =
                parameterResolver.resolve(
                        question,
                        definition
                );

        DatabaseQueryParameters parameters =
                parameterValidator.validate(
                        definition,
                        candidate
                );

        return executionService.execute(
                queryKey,
                parameters,
                currentUser
        );
    }

    private void validateInput(
            String question,
            CurrentUser currentUser
    ) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }

        if (currentUser == null) {
            throw new SecurityException(
                    "인증된 사용자 정보가 없습니다."
            );
        }
    }
}