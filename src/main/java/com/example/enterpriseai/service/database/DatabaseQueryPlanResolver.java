/*
 * =============================================================================
 * 클래스명 : DatabaseQueryPlanResolver
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL 대상 자연어 질문에서 구조화된 Query Plan 후보를 생성한다.
 *  - LLM에는 서버가 허용한 논리 필드와 최대 조회 범위만 전달한다.
 *  - SQL, Schema, Table, 실제 Column, queryKey 등의 내부 실행정보는 노출하지 않는다.
 *  - 생성 결과는 검증 전 DatabaseQueryPlanCandidate이며 직접 실행할 수 없다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryPlanCandidate;
import com.example.enterpriseai.dto.DatabaseQueryPlanValidationPolicy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseQueryPlanResolver {

    private final ChatClient chatClient;

    public DatabaseQueryPlanResolver(
            ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient = chatClientBuilder.build();
    }

    // 사용자 질문에서 서버 허용 범위 안의 Query Plan 후보만 생성한다.
    public DatabaseQueryPlanCandidate resolve(
            String question,
            DatabaseQueryPlanValidationPolicy policy
    ) {
        validateInput(question, policy);

        QueryPlanResolutionResponse response = chatClient.prompt()
                .system("""
                        사용자 질문을 Database 조회용 구조화 Query Plan 후보로 변환하세요.

                        반드시 제공된 논리 필드만 사용하세요.
                        사용자 질문에서 확인할 수 없는 조건이나 값을 임의로 생성하지 마세요.

                        filters는 단순 equality 조건만 표현하세요.
                        정렬이 필요한 경우 허용된 정렬 필드만 사용하세요.
                        GROUP BY는 현재 지원하지 않으므로 groupByFields는 비워 두세요.

                        SQL, Schema, Table, 실제 Column, queryKey,
                        Repository, Mapper 등의 내부 실행정보를 생성하지 마세요.

                        maxRows는 필요한 범위에서 지정하되
                        서버 최대 조회 건수를 초과하지 마세요.
                        """)
                .user("""
                        [사용자 질문]
                        %s

                        [허용 조회 필드]
                        %s

                        [허용 필터 필드]
                        %s

                        [허용 정렬 필드]
                        %s

                        [서버 최대 조회 건수]
                        %d
                        """.formatted(
                        question,
                        formatFields(policy.allowedSelectFields()),
                        formatFields(policy.allowedFilterFields()),
                        formatFields(policy.allowedOrderByFields()),
                        policy.maxRows()
                ))
                .call()
                .entity(
                        QueryPlanResolutionResponse.class,
                        spec -> spec.useProviderStructuredOutput()
                );

        return toCandidate(response);
    }

    private DatabaseQueryPlanCandidate toCandidate(
            QueryPlanResolutionResponse response
    ) {
        if (response == null) {
            throw new IllegalStateException(
                    "Query Plan 생성 결과가 없습니다."
            );
        }

        Map<String, Object> filters = new LinkedHashMap<>();

        if (response.filters() != null) {
            for (FilterValue filter : response.filters()) {
                if (filter == null
                        || filter.field() == null
                        || filter.field().isBlank()) {
                    continue;
                }

                if (filters.containsKey(filter.field())) {
                    throw new IllegalStateException(
                            "중복된 Query 필터 후보가 생성되었습니다."
                    );
                }

                filters.put(
                        filter.field(),
                        filter.value()
                );
            }
        }

        return new DatabaseQueryPlanCandidate(
                safeList(response.selectFields()),
                Map.copyOf(filters),
                safeList(response.groupByFields()),
                safeList(response.orderByFields()),
                response.maxRows()
        );
    }

    private List<String> safeList(List<String> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }

    private String formatFields(Iterable<String> fields) {
        StringBuilder context = new StringBuilder();

        for (String field : fields) {
            context.append("- ")
                    .append(field)
                    .append('\n');
        }

        return context.toString();
    }

    private void validateInput(
            String question,
            DatabaseQueryPlanValidationPolicy policy
    ) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "질문이 없습니다."
            );
        }

        if (policy == null) {
            throw new IllegalArgumentException(
                    "Query Plan 검증 정책이 없습니다."
            );
        }
    }

    // LLM Structured Output 전용 응답 구조이다.
    private record QueryPlanResolutionResponse(
            List<String> selectFields,
            List<FilterValue> filters,
            List<String> groupByFields,
            List<String> orderByFields,
            Integer maxRows
    ) {
    }

    private record FilterValue(
            String field,
            String value
    ) {
    }
}