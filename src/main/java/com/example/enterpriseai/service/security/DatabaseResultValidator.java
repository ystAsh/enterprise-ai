/*
 * =============================================================================
 * 클래스명 : DatabaseResultValidator
 * =============================================================================
 * 목적
 *  - Database RAG의 조회 결과가 LLM에 전달 가능한 안전한 데이터인지 검증한다.
 *  - 특정 회사, 업무, 테이블에 종속되지 않는 공통 결과 검증을 수행한다.
 */

package com.example.enterpriseai.service.security;

import com.example.enterpriseai.dto.DatabaseValidationPolicy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class DatabaseResultValidator {

    /*
     * 검증 정책에 따라 DB 조회 결과를 검사한다.
     *
     * 이 클래스는 데이터의 업무 의미를 판단하지 않는다.
     * 결과 구조와 LLM 전달 안전성만 검증한다.
     */
    public Map<String, Object> validate(
            Map<String, Object> data,
            DatabaseValidationPolicy policy
    ) {

        if (policy == null) {
            throw new IllegalArgumentException(
                    "DB 결과 검증 정책이 없습니다."
            );
        }

        if (data == null) {
            throw new IllegalStateException(
                    "DB 조회 결과가 없습니다."
            );
        }

        if (!policy.allowEmpty() && data.isEmpty()) {
            throw new IllegalStateException(
                    "DB 조회 결과가 비어 있습니다."
            );
        }

        if (data.size() > policy.maxFields()) {
            throw new IllegalStateException(
                    "DB 조회 결과의 필드 수가 허용 범위를 초과했습니다."
            );
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {

            validateFieldName(
                    entry.getKey(),
                    policy
            );

            validateValue(
                    entry.getValue(),
                    policy,
                    0
            );
        }

        return data;
    }

    /*
     * 결과 필드가 정책상 LLM 전달 가능한지 검증한다.
     */
    private void validateFieldName(
            String fieldName,
            DatabaseValidationPolicy policy
    ) {

        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalStateException(
                    "DB 조회 결과에 이름 없는 필드가 포함되어 있습니다."
            );
        }

        if (!policy.allowedFields().isEmpty()
                && !policy.allowedFields().contains(fieldName)) {

            throw new SecurityException(
                    "허용되지 않은 DB 결과 필드가 포함되어 있습니다."
            );
        }

        if (policy.sensitiveFields().contains(fieldName)) {
            throw new SecurityException(
                    "LLM 전달이 금지된 민감 필드가 포함되어 있습니다."
            );
        }
    }

    /*
     * 값의 타입, 크기, 중첩 깊이를 검증한다.
     */
    private void validateValue(
            Object value,
            DatabaseValidationPolicy policy,
            int depth
    ) {

        if (depth > policy.maxDepth()) {
            throw new IllegalStateException(
                    "DB 조회 결과의 중첩 구조가 허용 범위를 초과했습니다."
            );
        }

        if (value == null) {

            if (!policy.allowNullValues()) {
                throw new IllegalStateException(
                        "DB 조회 결과에 허용되지 않은 NULL 값이 포함되어 있습니다."
                );
            }

            return;
        }

        if (value instanceof String stringValue) {

            if (stringValue.length() > policy.maxStringLength()) {
                throw new IllegalStateException(
                        "DB 조회 결과 문자열이 허용 길이를 초과했습니다."
                );
            }

            return;
        }

        if (value instanceof Number
                || value instanceof Boolean
                || value instanceof Character) {
            return;
        }

        if (value instanceof Map<?, ?> mapValue) {

            if (mapValue.size() > policy.maxFields()) {
                throw new IllegalStateException(
                        "DB 조회 결과의 중첩 필드 수가 허용 범위를 초과했습니다."
                );
            }

            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {

                if (!(entry.getKey() instanceof String fieldName)) {
                    throw new IllegalStateException(
                            "DB 조회 결과의 필드 이름 형식이 올바르지 않습니다."
                    );
                }

                validateFieldName(
                        fieldName,
                        policy
                );

                validateValue(
                        entry.getValue(),
                        policy,
                        depth + 1
                );
            }

            return;
        }

        if (value instanceof Collection<?> collectionValue) {

            if (collectionValue.size() > policy.maxRows()) {
                throw new IllegalStateException(
                        "DB 조회 결과 건수가 허용 범위를 초과했습니다."
                );
            }

            for (Object item : collectionValue) {
                validateValue(
                        item,
                        policy,
                        depth + 1
                );
            }

            return;
        }

        throw new IllegalStateException(
                "LLM 전달이 허용되지 않은 DB 결과 타입입니다."
        );
    }
}