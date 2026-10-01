/*
 * =============================================================================
 * 클래스명 : DatabaseQueryRequestService
 * =============================================================================
 * 목적
 *  - 자연어 Database 질문을 서버의 검증된 Query 실행 흐름에 연결한다.
 *  - 등록된 Capability/Query를 항상 우선 사용한다.
 *  - 등록된 Query로 처리할 수 없는 경우에만 Safe Text-to-SQL로 fallback한다.
 *  - 권한/검증/실행 실패를 Safe Text-to-SQL로 우회하지 않는다.
 *  - Secure Verification 연결이 필요한 경우 검증 완료 실행 Context를 함께 반환한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryExecutionContext;
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
     * 기존 호출부 호환용이다.
     * Secure Verification 연결 정보가 필요한 호출부는 executeWithContext()를 사용한다.
     */
    public DatabaseQueryResult execute(
            String question,
            CurrentUser currentUser
    ) {
        return executeWithContext(
                question,
                currentUser
        ).queryResult();
    }

    /*
     * 등록 Query를 우선 사용하고 처리 가능한 Capability가 없을 때만
     * Safe Text-to-SQL로 fallback하여 공통 실행 Context를 반환한다.
     */
    public DatabaseQueryExecutionContext executeWithContext(
            String question,
            CurrentUser currentUser
    ) {
        validateInput(question, currentUser);

        Optional<String> queryKey =
                capabilityResolver.tryResolveQueryKey(
                        question
                );

        if (queryKey.isPresent()) {
            return executeRegisteredQueryWithContext(
                    question,
                    queryKey.get(),
                    currentUser
            );
        }

        return safeTextToSqlService.executeWithContext(
                question,
                currentUser
        );
    }

    // 기존 등록 Query의 Parameter Resolve/Validation 흐름을 그대로 유지한다.
    private DatabaseQueryExecutionContext executeRegisteredQueryWithContext(
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

        return executionService.executeWithContext(
                question,
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