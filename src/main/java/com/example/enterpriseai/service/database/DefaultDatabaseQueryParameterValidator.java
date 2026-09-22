/*
 * =============================================================================
 * 클래스명 : DefaultDatabaseQueryParameterValidator
 * =============================================================================
 * 목적
 *  - LLM 등이 생성한 검증 전 Query 파라미터 후보를 Java에서 검증한다.
 *  - 서버 Query Definition에 등록된 파라미터 정책만 허용한다.
 *  - 파라미터 타입, 필수 여부, 문자열 최대 길이를 서버 정책으로 검증한다.
 *  - 검증에 성공한 값만 DatabaseQueryParameters로 변환한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;
import com.example.enterpriseai.dto.DatabaseQueryParameterPolicy;
import com.example.enterpriseai.dto.DatabaseQueryParameters;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DefaultDatabaseQueryParameterValidator
        implements DatabaseQueryParameterValidator {

    @Override
    public DatabaseQueryParameters validate(
            DatabaseQueryDefinition definition,
            DatabaseQueryParameterCandidate candidate
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Query 정의가 없습니다."
            );
        }

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "Query 파라미터 후보가 없습니다."
            );
        }

        Map<String, DatabaseQueryParameterPolicy> parameterPolicies =
                definition.parameterPolicies();

        Map<String, Object> validatedValues = new HashMap<>();

        // 후보에 포함된 모든 파라미터를 서버 정책 기준으로 검증한다.
        for (Map.Entry<String, Object> entry : candidate.values().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            DatabaseQueryParameterPolicy policy =
                    getRequiredPolicy(key, parameterPolicies);

            validatedValues.put(
                    key,
                    validateAndNormalizeValue(value, policy)
            );
        }

        // required=true인 파라미터가 모두 존재하는지 검증한다.
        validateRequiredParameters(
                parameterPolicies,
                validatedValues
        );

        return new DatabaseQueryParameters(validatedValues);
    }

    // 등록되지 않은 파라미터 이름은 실행 단계로 전달하지 않는다.
    private DatabaseQueryParameterPolicy getRequiredPolicy(
            String key,
            Map<String, DatabaseQueryParameterPolicy> parameterPolicies
    ) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 파라미터 이름이 없습니다."
            );
        }

        DatabaseQueryParameterPolicy policy = parameterPolicies.get(key);

        if (policy == null) {
            throw new SecurityException(
                    "허용되지 않은 Query 파라미터입니다."
            );
        }

        return policy;
    }

    // 파라미터 타입과 값 제약을 서버 정책에 따라 검증한다.
    private Object validateAndNormalizeValue(
            Object value,
            DatabaseQueryParameterPolicy policy
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Query 파라미터 값이 없습니다."
            );
        }

        return switch (policy.type()) {
            case STRING -> validateString(value, policy);
            case LONG -> validateLong(value);
        };
    }

    // 문자열은 trim 처리 후 빈 값과 최대 길이를 검증한다.
    private String validateString(
            Object value,
            DatabaseQueryParameterPolicy policy
    ) {
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException(
                    "Query 파라미터 타입이 올바르지 않습니다."
            );
        }

        String normalized = stringValue.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 파라미터 값이 없습니다."
            );
        }

        if (normalized.length() > policy.maxLength()) {
            throw new IllegalArgumentException(
                    "Query 파라미터 값이 허용된 길이를 초과했습니다."
            );
        }

        return normalized;
    }

    // LONG 정책은 정수 객체 또는 숫자 문자열을 Long으로 정규화한다.
    private Long validateLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }

        if (value instanceof String stringValue) {
            String normalized = stringValue.trim();

            if (normalized.isBlank()) {
                throw new IllegalArgumentException(
                        "Query 파라미터 값이 없습니다."
                );
            }

            try {
                return Long.valueOf(normalized);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Query 파라미터 타입이 올바르지 않습니다.",
                        e
                );
            }
        }

        throw new IllegalArgumentException(
                "Query 파라미터 타입이 올바르지 않습니다."
        );
    }

    // required=true인 서버 등록 파라미터가 누락되면 실행을 차단한다.
    private void validateRequiredParameters(
            Map<String, DatabaseQueryParameterPolicy> parameterPolicies,
            Map<String, Object> validatedValues
    ) {
        for (Map.Entry<String, DatabaseQueryParameterPolicy> entry
                : parameterPolicies.entrySet()) {

            if (entry.getValue().required()
                    && !validatedValues.containsKey(entry.getKey())) {

                throw new IllegalArgumentException(
                        "필수 Query 파라미터가 없습니다."
                );
            }
        }
    }
}