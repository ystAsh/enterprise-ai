/*
 * =============================================================================
 * 클래스명 : DatabaseSafeTextToSqlServiceTest
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL에서 LLM이 생성한 필터보다 CurrentUser Mandatory Scope가
 *    우선 적용되는지 검증한다.
 *  - 다른 사용자 식별값이 Candidate에 포함되어도 현재 로그인 사용자 값으로
 *    강제 교체되는지 확인한다.
 *  - Gemini나 실제 DB 없이 Java 보안 경계를 deterministic하게 검증한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryCapability;
import com.example.enterpriseai.dto.DatabaseQueryPlan;
import com.example.enterpriseai.dto.DatabaseQueryPlanCandidate;
import com.example.enterpriseai.dto.DatabaseQueryPlanValidationPolicy;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.dto.DatabaseSafeQueryPolicy;
import com.example.enterpriseai.dto.DatabaseValidationPolicy;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.security.DatabaseQueryPlanValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseSafeTextToSqlServiceTest {

    private final DatabaseSafeQueryCapabilityResolver capabilityResolver =
            mock(DatabaseSafeQueryCapabilityResolver.class);

    private final DatabaseQueryPlanResolver queryPlanResolver =
            mock(DatabaseQueryPlanResolver.class);

    private final DatabaseQueryPlanValidator queryPlanValidator =
            mock(DatabaseQueryPlanValidator.class);

    private final DatabaseSafeQueryExecutionService executionService =
            mock(DatabaseSafeQueryExecutionService.class);

    private final DatabaseSafeTextToSqlService service =
            new DatabaseSafeTextToSqlService(
                    capabilityResolver,
                    queryPlanResolver,
                    queryPlanValidator,
                    executionService
            );

    @Test
    void executeOverridesCandidateUserIdWithCurrentUserScope() {
        CurrentUser currentUser =
                new CurrentUser(
                        10L,
                        "testuser",
                        "encoded-password",
                        1L,
                        1L,
                        3,
                        true,
                        true,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USER")
                        )
                );

        DatabaseQueryPlanValidationPolicy planValidationPolicy =
                new DatabaseQueryPlanValidationPolicy(
                        Set.of("username", "securityLevel"),
                        Set.of("userId"),
                        Set.of(),
                        Set.of(),
                        1
                );

        DatabaseValidationPolicy validationPolicy =
                new DatabaseValidationPolicy(
                        Set.of(
                                "rows",
                                "username",
                                "securityLevel"
                        ),
                        Set.of(),
                        10,
                        1,
                        500,
                        3,
                        true,
                        true
                );

        DatabaseSafeQueryPolicy safeQueryPolicy =
                new DatabaseSafeQueryPolicy(
                        "LOOKUP",
                        "SAFE_CURRENT_USER_LOOKUP",
                        "현재 사용자 기본 계정 정보 조회",
                        "enterprise-ai",
                        "SAFE_TEXT_TO_SQL",
                        "app_users",
                        Map.of(
                                "userId", "user_id",
                                "username", "username",
                                "securityLevel", "security_level"
                        ),
                        Set.of(),
                        1,
                        Map.of(
                                "userId",
                                DatabaseSafeQueryPolicy.CurrentUserScope.USER_ID
                        ),
                        validationPolicy
                );

        DatabaseQueryCapability capability =
                new DatabaseQueryCapability(
                        "CURRENT_USER_SAFE_QUERY",
                        "현재 로그인 사용자의 기본 계정 정보를 조회한다.",
                        Set.of("LOOKUP")
                );

        DatabaseSafeQueryPolicyRegistry.RegisteredSafeQueryCapability registered =
                new DatabaseSafeQueryPolicyRegistry.RegisteredSafeQueryCapability(
                        capability,
                        planValidationPolicy,
                        safeQueryPolicy
                );

        // LLM Candidate가 다른 사용자 ID를 만들었다고 가정한다.
        DatabaseQueryPlanCandidate candidate =
                new DatabaseQueryPlanCandidate(
                        List.of(
                                "username",
                                "securityLevel"
                        ),
                        Map.of(
                                "userId",
                                999L
                        ),
                        List.of(),
                        List.of(),
                        1
                );

        DatabaseQueryPlan validatedPlan =
                new DatabaseQueryPlan(
                        List.of(
                                "username",
                                "securityLevel"
                        ),
                        Map.of(
                                "userId",
                                currentUser.getUserId()
                        ),
                        List.of(),
                        List.of(),
                        1
                );

        DatabaseQueryResult expectedResult =
                mock(DatabaseQueryResult.class);

        when(capabilityResolver.resolve(
                "내 사용자명과 보안등급을 조회해줘."
        )).thenReturn(registered);

        when(queryPlanResolver.resolve(
                "내 사용자명과 보안등급을 조회해줘.",
                planValidationPolicy
        )).thenReturn(candidate);

        when(queryPlanValidator.validate(
                any(DatabaseQueryPlanCandidate.class),
                any(DatabaseQueryPlanValidationPolicy.class)
        )).thenReturn(validatedPlan);

        when(executionService.execute(
                validatedPlan,
                safeQueryPolicy
        )).thenReturn(expectedResult);

        DatabaseQueryResult result =
                service.execute(
                        "내 사용자명과 보안등급을 조회해줘.",
                        currentUser
                );

        ArgumentCaptor<DatabaseQueryPlanCandidate> candidateCaptor =
                ArgumentCaptor.forClass(
                        DatabaseQueryPlanCandidate.class
                );

        verify(queryPlanValidator).validate(
                candidateCaptor.capture(),
                any(DatabaseQueryPlanValidationPolicy.class)
        );

        DatabaseQueryPlanCandidate scopedCandidate =
                candidateCaptor.getValue();

        assertEquals(
                currentUser.getUserId(),
                scopedCandidate.filters().get("userId")
        );

        assertEquals(
                expectedResult,
                result
        );
    }
}