/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQueryCapabilityResolver
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL 대상 질문과 서버에 등록된 Safe Query Capability를 비교한다.
 *  - LLM에는 안전한 DatabaseQueryCapability 정보만 전달한다.
 *  - 실제 Table, Column, SQL, Query Plan 정책, Safe Query 정책 등의
 *    내부 실행정보는 노출하지 않는다.
 *  - 선택 결과를 Java Registry에서 다시 검증하고 서버 등록 정보만 반환한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryCapability;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabaseSafeQueryCapabilityResolver {

    private static final String NONE = "NONE";

    private final DatabaseSafeQueryPolicyRegistry policyRegistry;
    private final ChatClient chatClient;

    public DatabaseSafeQueryCapabilityResolver(
            DatabaseSafeQueryPolicyRegistry policyRegistry,
            ChatClient.Builder chatClientBuilder
    ) {
        this.policyRegistry = policyRegistry;
        this.chatClient = chatClientBuilder.build();
    }

    // 사용자 질문에 적합한 서버 등록 Safe Query Capability 하나를 선택한다.
    public DatabaseSafeQueryPolicyRegistry.RegisteredSafeQueryCapability resolve(
            String question
    ) {
        validateQuestion(question);

        List<DatabaseQueryCapability> capabilities =
                policyRegistry.findCapabilities();

        if (capabilities.isEmpty()) {
            throw new IllegalStateException(
                    "사용 가능한 Safe Query Capability가 없습니다."
            );
        }

        String capabilityContext =
                buildCapabilityContext(capabilities);

        String selectedCapabilityKey = chatClient.prompt()
                .system("""
                        사용자의 Database 질문을 처리할 수 있는
                        Safe Query Capability 하나를 선택하세요.

                        반드시 제공된 Capability 목록에 있는 기능만 선택하세요.

                        질문을 처리할 적절한 Capability가 없다면
                        NONE이라고 답변하세요.

                        SQL, Schema, Table, Column, queryKey,
                        Repository, Mapper, Query Plan 정책,
                        실행 정책 등의 내부 정보를 추측하거나 생성하지 마세요.

                        반드시 Capability 식별자 하나 또는
                        NONE만 답변하세요.
                        """)
                .user("""
                        [사용자 질문]
                        %s

                        [사용 가능한 Safe Query Capability]
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
            throw new IllegalStateException(
                    "질문을 처리할 수 있는 등록된 Safe Query Capability가 없습니다."
            );
        }

        // LLM 선택 결과를 서버 Registry에서 다시 검증한다.
        return policyRegistry.getRequired(
                normalizedCapabilityKey
        );
    }

    // LLM에는 자연어 선택에 필요한 안전한 Capability 정보만 전달한다.
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

    private String normalizeResult(String result) {
        if (result == null || result.isBlank()) {
            throw new IllegalStateException(
                    "Safe Query Capability 선택 결과가 없습니다."
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