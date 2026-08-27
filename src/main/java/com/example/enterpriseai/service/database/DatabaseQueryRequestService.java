/*
 * =============================================================================
 * 클래스명 : DatabaseQueryRequestService
 * =============================================================================
 * 목적
 *  - 자연어 Database 질문을 서버의 검증된 Query 실행 흐름에 연결한다.
 *  - Capability 선택, Query 파라미터 생성/검증, Query 실행 순서를 조정한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 *  - 실제 Query 실행 및 결과 검증은 하위 공통 계층에 위임한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;
import com.example.enterpriseai.dto.DatabaseQueryParameters;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class DatabaseQueryRequestService {

    private final DatabaseQueryCapabilityResolver capabilityResolver;
    private final DatabaseQueryDefinitionRegistry definitionRegistry;
    private final DatabaseQueryParameterResolverSelector parameterResolverSelector;
    private final DatabaseQueryParameterValidator parameterValidator;
    private final DatabaseQueryExecutionService executionService;

    public DatabaseQueryRequestService(
            DatabaseQueryCapabilityResolver capabilityResolver,
            DatabaseQueryDefinitionRegistry definitionRegistry,
            DatabaseQueryParameterResolverSelector parameterResolverSelector,
            DatabaseQueryParameterValidator parameterValidator,
            DatabaseQueryExecutionService executionService
    ) {
        this.capabilityResolver = capabilityResolver;
        this.definitionRegistry = definitionRegistry;
        this.parameterResolverSelector = parameterResolverSelector;
        this.parameterValidator = parameterValidator;
        this.executionService = executionService;
    }

    /*
     * 사용자 자연어 질문을 서버에 등록된 Database Query 실행 흐름으로 연결한다.
     */
    public DatabaseQueryResult execute(
            String question,
            CurrentUser currentUser
    ) {

        validateInput(
                question,
                currentUser
        );

        String queryKey =
                capabilityResolver.resolveQueryKey(
                        question
                );

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