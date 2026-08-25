/*
 * =============================================================================
 * 클래스명 : DatabaseQueryCapabilityResolver
 * =============================================================================
 * 목적
 *  - 사용자의 자연어 질문과 서버에 등록된 안전한 Database Query Capability를
 *    비교하여 가장 적합한 Capability 하나를 선택한다.
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
import java.util.stream.Collectors;

@Service
public class DatabaseQueryCapabilityResolver {

    private final DatabaseQueryCapabilityRegistry capabilityRegistry;
    private final ChatClient chatClient;

    public DatabaseQueryCapabilityResolver(
            DatabaseQueryCapabilityRegistry capabilityRegistry,
            ChatClient.Builder chatClientBuilder
    ) {
        this.capabilityRegistry = capabilityRegistry;
        this.chatClient = chatClientBuilder.build();
    }

    /*
     * 사용자 질문과 가장 적합한 Capability를 선택하고
     * 서버 내부 queryKey로 변환한다.
     *
     * LLM은 Capability 선택만 보조한다.
     * 실제 Query 실행 가능 여부는 이후 Java 보안 계층에서 다시 검증한다.
     */
    public String resolveQueryKey(
            String question
    ) {

        validateQuestion(question);

        List<DatabaseQueryCapability> capabilities =
                capabilityRegistry.findCapabilities();

        if (capabilities.isEmpty()) {
            throw new IllegalStateException(
                    "사용 가능한 Database Query Capability가 없습니다."
            );
        }

        String capabilityContext =
                buildCapabilityContext(
                        capabilities
                );

        String selectedCapabilityKey =
                chatClient.prompt()
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
                normalizeResult(
                        selectedCapabilityKey
                );

        if ("NONE".equals(normalizedCapabilityKey)) {
            throw new IllegalStateException(
                    "질문을 처리할 수 있는 등록된 Database Query Capability가 없습니다."
            );
        }

        /*
         * LLM이 반환한 capabilityKey를 그대로 신뢰하지 않는다.
         *
         * Registry에서 다시 확인하고 서버에 등록된 Capability만
         * 실제 queryKey로 변환한다.
         */
        return capabilityRegistry.getRequiredQueryKey(
                normalizedCapabilityKey
        );
    }

    /*
     * LLM에 전달할 최소 Capability Context를 생성한다.
     *
     * queryKey, executionType, SQL, Policy 등의
     * 서버 내부 실행 정보는 포함하지 않는다.
     */
    private String buildCapabilityContext(
            List<DatabaseQueryCapability> capabilities
    ) {

        return capabilities.stream()
                .map(capability ->
                        """
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
                        )
                )
                .collect(
                        Collectors.joining("\n")
                );
    }

    /*
     * LLM 응답을 최소한으로 정규화한다.
     *
     * 실제 등록 여부는 Registry가 최종 판단한다.
     */
    private String normalizeResult(
            String result
    ) {

        if (result == null || result.isBlank()) {
            throw new IllegalStateException(
                    "Database Query Capability 선택 결과가 없습니다."
            );
        }

        return result.trim();
    }

    private void validateQuestion(
            String question
    ) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }
    }
}