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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

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

        Map<String, Object> values = chatClient.prompt()
                .system("""
                        사용자의 질문에서 Database 조회에 필요한
                        파라미터 후보값만 추출하세요.

                        제공된 파라미터 목록의 key만 사용하세요.

                        특정 회사의 업무 용어, 코드 체계, 식별자 형식,
                        접두사, 숫자 규칙을 임의로 가정하지 마세요.

                        사용자 질문에 실제로 포함된 값과
                        제공된 파라미터 설명만 기준으로 판단하세요.

                        SQL, 테이블, 컬럼, Schema, Query 실행정보를
                        추측하거나 생성하지 마세요.

                        값을 확인할 수 없는 선택 파라미터는 생략하세요.
                        필수 파라미터도 값을 확인할 수 없으면
                        임의 값을 생성하지 마세요.
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
                        new ParameterizedTypeReference<
                                Map<String, Object>
                                >() {
                        },
                        spec -> spec.useProviderStructuredOutput()
                );

        return new DatabaseQueryParameterCandidate(
                values == null ? Map.of() : values
        );
    }

    // LLM에는 파라미터 후보 추출에 필요한 안전한 정책 정보만 전달한다.
    private String buildParameterContext(
            Map<String, DatabaseQueryParameterPolicy> parameterPolicies
    ) {
        StringBuilder context = new StringBuilder();

        parameterPolicies.forEach((key, policy) -> {
            context.append("- key: ")
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
}