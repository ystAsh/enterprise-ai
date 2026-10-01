/*
 * =============================================================================
 * 클래스명 : DatabaseSafeTextToSqlService
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL 전체 흐름을 조정한다.
 *  - Safe Query Capability 선택, Query Plan 후보 생성/검증,
 *    사용자 Mandatory Scope 강제, Safe Query 실행 및 결과 검증 순서를 관리한다.
 *  - LLM이 생성한 Candidate를 직접 실행하지 않는다.
 *  - 서버 등록 정책과 Java 검증을 통과한 결과만 DatabaseQueryResult로 반환한다.
 *  - Secure Verification 연결이 필요한 경우 내부 실행 Context를 함께 반환한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryExecutionContext;
import com.example.enterpriseai.dto.DatabaseQueryPlan;
import com.example.enterpriseai.dto.DatabaseQueryPlanCandidate;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.dto.DatabaseSafeQueryPolicy;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.security.DatabaseQueryPlanValidator;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DatabaseSafeTextToSqlService {

    private final DatabaseSafeQueryCapabilityResolver capabilityResolver;
    private final DatabaseQueryPlanResolver queryPlanResolver;
    private final DatabaseQueryPlanValidator queryPlanValidator;
    private final DatabaseSafeQueryExecutionService executionService;

    public DatabaseSafeTextToSqlService(
            DatabaseSafeQueryCapabilityResolver capabilityResolver,
            DatabaseQueryPlanResolver queryPlanResolver,
            DatabaseQueryPlanValidator queryPlanValidator,
            DatabaseSafeQueryExecutionService executionService
    ) {
        this.capabilityResolver = capabilityResolver;
        this.queryPlanResolver = queryPlanResolver;
        this.queryPlanValidator = queryPlanValidator;
        this.executionService = executionService;
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

    // Safe Text-to-SQL 전체 흐름을 실행하고 검증 완료 결과와 내부 실행 Context를 반환한다.
    public DatabaseQueryExecutionContext executeWithContext(
            String question,
            CurrentUser currentUser
    ) {
        validateInput(question, currentUser);

        DatabaseSafeQueryPolicyRegistry.RegisteredSafeQueryCapability registered =
                capabilityResolver.resolve(question);

        DatabaseQueryPlanCandidate candidate =
                queryPlanResolver.resolve(
                        question,
                        registered.planValidationPolicy()
                );

        DatabaseQueryPlanCandidate scopedCandidate =
                applyMandatoryScopes(
                        candidate,
                        registered.safeQueryPolicy(),
                        currentUser
                );

        DatabaseQueryPlan plan =
                queryPlanValidator.validate(
                        scopedCandidate,
                        registered.planValidationPolicy()
                );

        return executionService.executeWithContext(
                plan,
                registered.safeQueryPolicy()
        );
    }

    // LLM Candidate보다 서버의 CurrentUser 기반 Mandatory Scope를 우선 적용한다.
    private DatabaseQueryPlanCandidate applyMandatoryScopes(
            DatabaseQueryPlanCandidate candidate,
            DatabaseSafeQueryPolicy policy,
            CurrentUser currentUser
    ) {
        if (candidate == null) {
            throw new IllegalStateException(
                    "Query Plan 후보가 없습니다."
            );
        }

        Map<String, Object> filters =
                new LinkedHashMap<>();

        if (candidate.filters() != null) {
            filters.putAll(candidate.filters());
        }

        policy.mandatoryScopes().forEach((logicalField, scope) ->
                filters.put(
                        logicalField,
                        resolveScopeValue(
                                scope,
                                currentUser
                        )
                )
        );

        return new DatabaseQueryPlanCandidate(
                candidate.selectFields(),
                Map.copyOf(filters),
                candidate.groupByFields(),
                candidate.orderByFields(),
                candidate.maxRows()
        );
    }

    private Object resolveScopeValue(
            DatabaseSafeQueryPolicy.CurrentUserScope scope,
            CurrentUser currentUser
    ) {
        return switch (scope) {
            case USER_ID -> currentUser.getUserId();
            case ORGANIZATION_ID -> currentUser.getOrganizationId();
            case DEPARTMENT_ID -> currentUser.getDepartmentId();
        };
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