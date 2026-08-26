/*
 * =============================================================================
 * 클래스명 : DefaultDatabaseQueryParameterResolver
 * =============================================================================
 * 목적
 *  - 선택 완료된 Query Definition을 기준으로 사용자 질문에서
 *    Query 실행 파라미터 후보를 추출한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 *  - LLM이 생성한 값은 검증 전 후보이며 직접 실행하지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DefaultDatabaseQueryParameterResolver
        implements DatabaseQueryParameterResolver {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public DefaultDatabaseQueryParameterResolver(
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.chatClient =
                chatClientBuilder.build();

        this.objectMapper =
                objectMapper;
    }

    @Override
    public DatabaseQueryParameterCandidate resolve(
            String question,
            DatabaseQueryDefinition definition
    ) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "사용자 질문이 없습니다."
            );
        }

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Query 정의가 없습니다."
            );
        }

        String response = chatClient.prompt()
                .system("""
                        사용자의 자연어 질문에서
                        기존 시스템 조회에 필요한 실행 파라미터 후보만 추출하세요.

                        반드시 다음 규칙을 지키세요.

                        - SQL을 생성하지 마세요.
                        - 테이블이나 컬럼을 추측하지 마세요.
                        - Repository나 Mapper를 추측하지 마세요.
                        - 권한정보를 생성하지 마세요.
                        - 실행방식을 결정하지 마세요.
                        - 질문에서 확인할 수 없는 값은 만들지 마세요.
                        - 설명 문장을 작성하지 마세요.
                        - JSON 객체만 반환하세요.
                        """)
                .user("""
                        [사용자 질문]
                        %s

                        [Query 설명]
                        %s
                        """.formatted(
                        question.trim(),
                        definition.queryName()
                ))
                .call()
                .content();

        return new DatabaseQueryParameterCandidate(
                parseCandidate(response)
        );
    }

    private Map<String, Object> parseCandidate(
            String response
    ) {

        if (response == null || response.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(
                    response.trim(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Query 파라미터 후보를 해석할 수 없습니다.",
                    e
            );
        }
    }
}