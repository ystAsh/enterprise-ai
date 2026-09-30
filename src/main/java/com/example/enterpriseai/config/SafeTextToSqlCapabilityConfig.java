/*
 * =============================================================================
 * 클래스명 : SafeTextToSqlCapabilityConfig
 * =============================================================================
 * 목적
 *  - enterprise-ai가 직접 소유한 MSSQL 데이터에 대한 Safe Text-to-SQL
 *    Capability와 서버 내부 정책을 등록한다.
 *  - LLM에는 안전한 Capability 정보만 노출한다.
 *  - 실제 Table, Column Mapping, Mandatory Scope, 결과 검증 정책은
 *    서버 내부에서만 관리한다.
 */

package com.example.enterpriseai.config;

import com.example.enterpriseai.dto.DatabaseQueryCapability;
import com.example.enterpriseai.dto.DatabaseQueryPlanValidationPolicy;
import com.example.enterpriseai.dto.DatabaseSafeQueryPolicy;
import com.example.enterpriseai.dto.DatabaseValidationPolicy;
import com.example.enterpriseai.service.database.DatabaseSafeQueryPolicyRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@Configuration
public class SafeTextToSqlCapabilityConfig {

    @Bean
    public DatabaseSafeQueryPolicyRegistry.RegisteredSafeQueryCapability
    currentUserSafeQueryCapability() {

        DatabaseQueryCapability capability =
                new DatabaseQueryCapability(
                        "CURRENT_USER_SAFE_QUERY",
                        "현재 로그인 사용자의 기본 계정 정보를 조회한다.",
                        Set.of("LOOKUP")
                );

        DatabaseQueryPlanValidationPolicy planValidationPolicy =
                new DatabaseQueryPlanValidationPolicy(
                        Set.of(
                                "username",
                                "organizationId",
                                "departmentId",
                                "securityLevel",
                                "enabled"
                        ),
                        Set.of(
                                "userId"
                        ),
                        Set.of(),
                        Set.of(
                                "username"
                        ),
                        1
                );

        DatabaseValidationPolicy validationPolicy =
                new DatabaseValidationPolicy(
                        Set.of(
                                "rows",
                                "username",
                                "organizationId",
                                "departmentId",
                                "securityLevel",
                                "enabled"
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
                                "organizationId", "organization_id",
                                "departmentId", "department_id",
                                "securityLevel", "security_level",
                                "enabled", "enabled"
                        ),
                        Set.of(
                                "username"
                        ),
                        1,
                        Map.of(
                                "userId",
                                DatabaseSafeQueryPolicy.CurrentUserScope.USER_ID
                        ),
                        validationPolicy
                );

        return new DatabaseSafeQueryPolicyRegistry.RegisteredSafeQueryCapability(
                capability,
                planValidationPolicy,
                safeQueryPolicy
        );
    }
}