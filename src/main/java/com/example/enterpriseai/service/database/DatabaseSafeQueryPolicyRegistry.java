/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQueryPolicyRegistry
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL용 자연어 Capability와 서버 내부 Safe Query 정책을 연결한다.
 *  - Query Plan 검증 정책과 실제 Safe Query 실행 정책을 서버에서 함께 등록한다.
 *  - LLM에는 안전한 DatabaseQueryCapability 정보만 노출한다.
 *  - 실제 Table, Column Mapping, Result Validation Policy 등의 내부 실행정보는
 *    서버 Registry 내부에서만 관리한다.
 *  - 서버에 등록되지 않은 Safe Query Capability의 실행을 차단한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryCapability;
import com.example.enterpriseai.dto.DatabaseQueryPlanValidationPolicy;
import com.example.enterpriseai.dto.DatabaseSafeQueryPolicy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseSafeQueryPolicyRegistry {

    private final Map<String, RegisteredSafeQueryCapability> capabilities;

    public DatabaseSafeQueryPolicyRegistry(
            List<RegisteredSafeQueryCapability> registeredCapabilities
    ) {
        Map<String, RegisteredSafeQueryCapability> registry =
                new HashMap<>();

        for (RegisteredSafeQueryCapability registeredCapability
                : registeredCapabilities) {

            if (registeredCapability == null) {
                throw new IllegalStateException(
                        "등록할 Safe Query Capability가 null입니다."
                );
            }

            String capabilityKey =
                    registeredCapability.capability()
                            .capabilityKey();

            if (registry.containsKey(capabilityKey)) {
                throw new IllegalStateException(
                        "중복된 Safe Query Capability가 등록되었습니다."
                );
            }

            registry.put(
                    capabilityKey,
                    registeredCapability
            );
        }

        this.capabilities =
                Collections.unmodifiableMap(registry);
    }

    // 자연어 선택에 사용할 안전한 Capability 정보만 반환한다.
    public List<DatabaseQueryCapability> findCapabilities() {
        return capabilities.values()
                .stream()
                .map(RegisteredSafeQueryCapability::capability)
                .toList();
    }

    // 선택된 Capability의 서버 내부 등록 정보를 반환한다.
    public RegisteredSafeQueryCapability getRequired(
            String capabilityKey
    ) {
        if (capabilityKey == null || capabilityKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Safe Query Capability 식별자가 없습니다."
            );
        }

        RegisteredSafeQueryCapability registeredCapability =
                capabilities.get(capabilityKey);

        if (registeredCapability == null) {
            throw new SecurityException(
                    "등록되지 않은 Safe Query Capability입니다."
            );
        }

        return registeredCapability;
    }

    /*
     * Safe Text-to-SQL용 서버 내부 등록 정보이다.
     *
     * capability만 LLM 선택에 사용하며
     * planValidationPolicy와 safeQueryPolicy는 LLM에 전달하지 않는다.
     */
    public record RegisteredSafeQueryCapability(
            DatabaseQueryCapability capability,
            DatabaseQueryPlanValidationPolicy planValidationPolicy,
            DatabaseSafeQueryPolicy safeQueryPolicy
    ) {

        public RegisteredSafeQueryCapability {
            if (capability == null) {
                throw new IllegalArgumentException(
                        "Safe Query Capability가 없습니다."
                );
            }

            if (planValidationPolicy == null) {
                throw new IllegalArgumentException(
                        "Query Plan 검증 정책이 없습니다."
                );
            }

            if (safeQueryPolicy == null) {
                throw new IllegalArgumentException(
                        "Safe Query 실행 정책이 없습니다."
                );
            }

            if (planValidationPolicy.maxRows()
                    > safeQueryPolicy.maxRows()) {
                throw new IllegalArgumentException(
                        "Query Plan 최대 건수는 Safe Query 최대 건수를 초과할 수 없습니다."
                );
            }
        }
    }
}