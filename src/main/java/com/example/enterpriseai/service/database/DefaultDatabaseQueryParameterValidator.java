/*
 * =============================================================================
 * 클래스명 : DefaultDatabaseQueryParameterValidator
 * =============================================================================
 * 목적
 *  - LLM 등이 생성한 검증 전 Query 파라미터 후보를 Java에서 검증한다.
 *  - 서버 Query Definition에 등록된 파라미터만 허용한다.
 *  - 검증에 성공한 값만 DatabaseQueryParameters로 변환한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;
import com.example.enterpriseai.dto.DatabaseQueryParameters;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

        Set<String> allowedParameterKeys =
                definition.allowedParameterKeys();

        Map<String, Object> validatedValues =
                new HashMap<>();

        for (Map.Entry<String, Object> entry
                : candidate.values().entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            validateKey(
                    key,
                    allowedParameterKeys
            );

            validatedValues.put(
                    key,
                    validateAndNormalizeValue(value)
            );
        }

        return new DatabaseQueryParameters(
                validatedValues
        );
    }

    private void validateKey(
            String key,
            Set<String> allowedParameterKeys
    ) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Query 파라미터 이름이 없습니다."
            );
        }

        if (!allowedParameterKeys.contains(key)) {
            throw new SecurityException(
                    "허용되지 않은 Query 파라미터입니다."
            );
        }
    }

    private Object validateAndNormalizeValue(
            Object value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Query 파라미터 값이 없습니다."
            );
        }

        if (value instanceof String stringValue) {

            String normalized =
                    stringValue.trim();

            if (normalized.isBlank()) {
                throw new IllegalArgumentException(
                        "Query 파라미터 값이 없습니다."
                );
            }

            return normalized;
        }

        return value;
    }
}