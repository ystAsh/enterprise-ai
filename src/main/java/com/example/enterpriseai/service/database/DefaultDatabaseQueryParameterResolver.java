/*
 * =============================================================================
 * 클래스명 : DefaultDatabaseQueryParameterResolver
 * =============================================================================
 * 목적
 *  - 서버에서 확정된 Query Definition의 파라미터 정책을 이용해
 *    사용자 질문에서 실행 파라미터 후보를 생성한다.
 *  - LLM에는 안전한 파라미터 설명만 전달하며 내부 Query 실행정보는 노출하지 않는다.
 *  - 특정 회사의 업무 용어, 식별자 형식, 코드 규칙을 공통 계층에서 가정하지 않는다.
 *  - 생성 결과는 검증 전 Candidate이며 직접 Query 실행에 사용하지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;
import com.example.enterpriseai.dto.DatabaseQueryParameterPolicy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultDatabaseQueryParameterResolver
        implements DatabaseQueryParameterResolver {

    private final ChatClient chatClient;

    public DefaultDatabaseQueryParameterResolver(
            ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public boolean supports(DatabaseQueryDefinition definition) {
        return definition != null
                && definition.parameterPolicies() != null
                && !definition.parameterPolicies().isEmpty();
    }

    @Override
    public DatabaseQueryParameterCandidate resolve(
            String question,
            DatabaseQueryDefinition definition
    ) {
        if (!supports(definition)) {
            throw new IllegalArgumentException(
                    "지원할 수 있는 Query 파라미터 정책이 없습니다."
            );
        }

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }

        String parameterContext =
                buildParameterContext(definition.parameterPolicies());

        ParameterResolutionResponse response = chatClient.prompt()
                .system("""
                        사용자 질문에서 Database 조회에 필요한
                        파라미터 후보값만 추출하세요.

                        제공된 파라미터 이름만 사용할 수 있습니다.
                        파라미터 값은 사용자 질문에서 확인 가능한 값만 사용하세요.

                        특정 회사의 코드 체계나 식별자 형식을 임의로 가정하지 마세요.
                        SQL, 테이블, 컬럼, Schema, Query 실행정보를 생성하지 마세요.

                        값을 확인할 수 없는 파라미터는 결과에서 제외하세요.
                        """)
                .user("""
                        [사용자 질문]
                        %s

                        [허용된 파라미터]
                        %s
                        """.formatted(
                        question,
                        parameterContext
                ))
                .call()
                .entity(
                        ParameterResolutionResponse.class,
                        spec -> spec.useProviderStructuredOutput()
                );

        return new DatabaseQueryParameterCandidate(
                toCandidateValues(response)
        );
    }

    // LLM에는 후보 추출에 필요한 안전한 파라미터 정책만 전달한다.
    private String buildParameterContext(
            Map<String, DatabaseQueryParameterPolicy> parameterPolicies
    ) {
        StringBuilder context = new StringBuilder();

        parameterPolicies.forEach((key, policy) -> {
            context.append("- name: ")
                    .append(key)
                    .append(", description: ")
                    .append(policy.description())
                    .append(", type: ")
                    .append(policy.type())
                    .append(", required: ")
                    .append(policy.required());

            if (policy.maxLength() != null) {
                context.append(", maxLength: ")
                        .append(policy.maxLength());
            }

            context.append('\n');
        });

        return context.toString();
    }

    // Structured Output도 신뢰하지 않고 Candidate Map으로만 변환한다.
    private Map<String, Object> toCandidateValues(
            ParameterResolutionResponse response
    ) {
        if (response == null || response.parameters() == null) {
            return Map.of();
        }

        Map<String, Object> values = new LinkedHashMap<>();

        for (ParameterValue parameter : response.parameters()) {
            if (parameter == null
                    || parameter.name() == null
                    || parameter.name().isBlank()) {
                continue;
            }

            if (values.containsKey(parameter.name())) {
                throw new IllegalStateException(
                        "중복된 Query 파라미터 후보가 생성되었습니다."
                );
            }

            values.put(
                    parameter.name(),
                    parameter.value()
            );
        }

        return values;
    }

    // LLM Structured Output의 고정 응답 구조이다.
    private record ParameterResolutionResponse(
            List<ParameterValue> parameters
    ) {
    }

    private record ParameterValue(
            String name,
            String value
    ) {
    }
}