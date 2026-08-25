/*
 * =============================================================================
 * 클래스명 : DatabaseQueryCapabilityRegistry
 * =============================================================================
 * 목적
 *  - 자연어 질문 검색용 Capability와 서버 내부 Query Definition을 연결한다.
 *  - LLM에 실제 queryKey나 Query 실행 정보를 노출하지 않는다.
 *  - 서버에 등록된 Capability만 조회할 수 있도록 관리한다.
 *  - 특정 회사, 업무, 테이블, Repository, Mapper에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryCapability;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseQueryCapabilityRegistry {

    private final Map<String, RegisteredCapability> capabilities;

    public DatabaseQueryCapabilityRegistry(
            List<RegisteredCapability> registeredCapabilities
    ) {

        Map<String, RegisteredCapability> registry =
                new HashMap<>();

        for (RegisteredCapability registeredCapability
                : registeredCapabilities) {

            if (registeredCapability == null) {
                throw new IllegalStateException(
                        "등록할 Database Query Capability가 null입니다."
                );
            }

            String capabilityKey =
                    registeredCapability.capability()
                            .capabilityKey();

            if (registry.containsKey(capabilityKey)) {
                throw new IllegalStateException(
                        "중복된 Database Query Capability가 등록되었습니다."
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

    /*
     * 자연어 질문과 매칭할 수 있는 안전한 Capability 목록만 반환한다.
     *
     * 실제 queryKey는 반환하지 않는다.
     */
    public List<DatabaseQueryCapability> findCapabilities() {

        return capabilities.values()
                .stream()
                .map(RegisteredCapability::capability)
                .toList();
    }

    /*
     * 선택된 Capability를 실제 서버 내부 queryKey로 변환한다.
     *
     * 이 메서드는 Java 서버 내부 실행 경계에서만 사용한다.
     */
    public String getRequiredQueryKey(
            String capabilityKey
    ) {

        if (capabilityKey == null
                || capabilityKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Capability 식별자가 없습니다."
            );
        }

        RegisteredCapability registeredCapability =
                capabilities.get(capabilityKey);

        if (registeredCapability == null) {
            throw new SecurityException(
                    "등록되지 않은 Database Query Capability입니다."
            );
        }

        return registeredCapability.queryKey();
    }

    /*
     * Capability와 실제 Query의 서버 내부 연결 정보이다.
     *
     * queryKey는 LLM용 Capability 정보에 포함하지 않는다.
     */
    public record RegisteredCapability(

            DatabaseQueryCapability capability,

            String queryKey

    ) {

        public RegisteredCapability {

            if (capability == null) {
                throw new IllegalArgumentException(
                        "Database Query Capability가 없습니다."
                );
            }

            if (queryKey == null || queryKey.isBlank()) {
                throw new IllegalArgumentException(
                        "연결할 Query 식별자가 없습니다."
                );
            }
        }
    }
}