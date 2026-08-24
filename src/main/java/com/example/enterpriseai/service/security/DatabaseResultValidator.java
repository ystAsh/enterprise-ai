/*
 * =============================================================================
 * 클래스명 : DatabaseResultValidator
 * =============================================================================
 * 목적
 *  - Database RAG의 조회 결과가 LLM에 전달 가능한 안전한 데이터인지 검증한다.
 *  - 특정 회사, 업무, 테이블, 컬럼 의미에 종속되지 않는 공통 검증을 수행한다.
 *  - 허용되지 않은 필드, 과도한 결과, 민감정보, 비정상 구조를 차단한다.
 */

package com.example.enterpriseai.service.security;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Service
public class DatabaseResultValidator {

    /*
     * DB 조회 결과를 공통 ValidationPolicy 기준으로 검증한다.
     *
     * 이 Validator는 결과가 직원/매출/재고/제품인지 알지 못한다.
     * 오직 데이터 구조와 보안 정책만 검증한다.
     */
    public Map<String, Object> validate(
            Map<String, Object> data,
            ValidationPolicy policy
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

        validateFields(
                data,
                policy
        );

        return data;
    }

    /*
     * 최상위 결과 필드의 허용 여부와 값을 검증한다.
     */
    private void validateFields(
            Map<String, Object> data,
            ValidationPolicy policy
    ) {

        for (Map.Entry<String, Object> entry : data.entrySet()) {

            String fieldName = entry.getKey();
            Object value = entry.getValue();

            validateFieldName(
                    fieldName,
                    policy
            );

            validateValue(
                    value,
                    policy,
                    0
            );
        }
    }

    /*
     * 허용되지 않은 필드와 민감 필드를 차단한다.
     */
    private void validateFieldName(
            String fieldName,
            ValidationPolicy policy
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
     * 결과 값의 타입, 크기, 중첩 깊이를 공통 검증한다.
     */
    private void validateValue(
            Object value,
            ValidationPolicy policy,
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

            if (stringValue.length()
                    > policy.maxStringLength()) {

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

                if (!(entry.getKey() instanceof String nestedFieldName)) {
                    throw new IllegalStateException(
                            "DB 조회 결과의 필드 이름 형식이 올바르지 않습니다."
                    );
                }

                validateFieldName(
                        nestedFieldName,
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

            if (collectionValue.size()
                    > policy.maxCollectionSize()) {

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
                "LLM 전달이 허용되지 않은 DB 결과 타입입니다: "
                        + value.getClass().getSimpleName()
        );
    }

    /*
     * DB 결과에 적용할 공통 보안/구조 검증 정책이다.
     *
     * 업무 의미가 아니라 LLM 전달 가능 범위만 정의한다.
     */
    public record ValidationPolicy(

            Set<String> allowedFields,
            Set<String> sensitiveFields,

            int maxFields,
            int maxCollectionSize,
            int maxStringLength,
            int maxDepth,

            boolean allowNullValues,
            boolean allowEmpty
    ) {

        public ValidationPolicy {

            allowedFields =
                    allowedFields == null
                            ? Set.of()
                            : Set.copyOf(allowedFields);

            sensitiveFields =
                    sensitiveFields == null
                            ? Set.of()
                            : Set.copyOf(sensitiveFields);

            if (maxFields <= 0) {
                throw new IllegalArgumentException(
                        "maxFields는 1 이상이어야 합니다."
                );
            }

            if (maxCollectionSize <= 0) {
                throw new IllegalArgumentException(
                        "maxCollectionSize는 1 이상이어야 합니다."
                );
            }

            if (maxStringLength <= 0) {
                throw new IllegalArgumentException(
                        "maxStringLength는 1 이상이어야 합니다."
                );
            }

            if (maxDepth < 0) {
                throw new IllegalArgumentException(
                        "maxDepth는 0 이상이어야 합니다."
                );
            }
        }
    }
}