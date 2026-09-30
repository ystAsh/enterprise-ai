/*
 * =============================================================================
 * 클래스명 : DatabaseQueryCapabilityResolver
 * =============================================================================
 * 목적
 *  - 사용자의 자연어 질문과 서버에 등록된 안전한 Database Query Capability를
 *    비교하여 가장 적합한 Capability 하나를 선택한다.
 *  - 기존 Database RAG에서는 등록 Capability 우선 실행 방식을 유지한다.
 *  - Safe Text-to-SQL fallback 판단 시에는 적합한 Capability가 없음을 명시적으로 반환한다.
 *  - 실제 queryKey, SQL, Schema, Repository, Mapper, 권한 정책 등의
 *    서버 내부 실행 정보를 LLM에 전달하지 않는다.
 *  - LLM의 선택 결과를 Java에서 서버 등록 Capability인지 다시 검증한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryCapability;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DatabaseQueryCapabilityResolver {

    private static final String NONE = "NONE";

    private final DatabaseQueryCapabilityRegistry capabilityRegistry;
    private final ChatClient chatClient;

    public DatabaseQueryCapabilityResolver(
            DatabaseQueryCapabilityRegistry capabilityRegistry,
            ChatClient.Builder chatClientBuilder
    ) {
        this.capabilityRegistry = capabilityRegistry;
        this.chatClient = chatClientBuilder.build();
    }

    // 기존 Database RAG에서 사용할 Query Key를 확정한다.
    public String resolveQueryKey(String question) {
        validateQuestion(question);

        List<DatabaseQueryCapability> capabilities =
                capabilityRegistry.findCapabilities();

        if (capabilities.isEmpty()) {
            throw new IllegalStateException(
                    "사용 가능한 Database Query Capability가 없습니다."
            );
        }

        // 기존 Phase 10 동작은 유지한다.
        if (capabilities.size() == 1) {
            return capabilityRegistry.getRequiredQueryKey(
                    capabilities.getFirst().capabilityKey()
            );
        }

        return resolveRegisteredQueryKey(
                question,
                capabilities
        ).orElseThrow(
                () -> new IllegalStateException(
                        "질문을 처리할 수 있는 등록된 Database Query Capability가 없습니다."
                )
        );
    }

    /*
     * Safe Text-to-SQL fallback 판단용이다.
     *
     * 등록 Capability가 있어도 질문 적합성을 확인하며,
     * 적합한 Capability가 없으면 예외가 아닌 Optional.empty()를 반환한다.
     */
    public Optional<String> tryResolveQueryKey(String question) {
        validateQuestion(question);

        List<DatabaseQueryCapability> capabilities =
                capabilityRegistry.findCapabilities();

        if (capabilities.isEmpty()) {
            return Optional.empty();
        }

        return resolveRegisteredQueryKey(
                question,
                capabilities
        );
    }

    // LLM 선택 결과가 서버 등록 Capability이면 내부 queryKey로 변환한다.
    private Optional<String> resolveRegisteredQueryKey(
            String question,
            List<DatabaseQueryCapability> capabilities
    ) {
        String capabilityContext =
                buildCapabilityContext(capabilities);

        String selectedCapabilityKey = chatClient.prompt()
                .system("""
                        사용자의 질문을 처리하기에 가장 적합한
                        Database Query Capability 하나를 선택하세요.

                        제공된 Capability 목록에 있는 기능만 선택할 수 있습니다.

                        실제 SQL, 테이블, 컬럼, 데이터베이스 구조를
                        추측하지 마세요.

                        질문을 처리할 적절한 Capability가 없다면
                        NONE이라고 답변하세요.

                        반드시 Capability 식별자 하나 또는
                        NONE만 답변하세요.
                        """)
                .user("""
                        [사용자 질문]
                        %s

                        [사용 가능한 Capability]
                        %s
                        """.formatted(
                        question,
                        capabilityContext
                ))
                .call()
                .content();

        String normalizedCapabilityKey =
                normalizeResult(selectedCapabilityKey);

        if (NONE.equals(normalizedCapabilityKey)) {
            return Optional.empty();
        }

        // LLM 결과는 Registry에서 다시 확인하고 등록된 Capability만 허용한다.
        return Optional.of(
                capabilityRegistry.getRequiredQueryKey(
                        normalizedCapabilityKey
                )
        );
    }

    // LLM에는 Capability 선택에 필요한 안전한 정보만 전달한다.
    private String buildCapabilityContext(
            List<DatabaseQueryCapability> capabilities
    ) {
        return capabilities.stream()
                .map(capability -> """
                        capabilityKey: %s
                        description: %s
                        supportedIntents: %s
                        """.formatted(
                        capability.capabilityKey(),
                        capability.description(),
                        String.join(
                                ", ",
                                capability.supportedIntents()
                        )
                ))
                .collect(Collectors.joining("\n"));
    }

    // 실제 등록 여부는 Registry가 최종 판단한다.
    private String normalizeResult(String result) {
        if (result == null || result.isBlank()) {
            throw new IllegalStateException(
                    "Database Query Capability 선택 결과가 없습니다."
            );
        }

        return result.trim();
    }

    private void validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }
    }
}