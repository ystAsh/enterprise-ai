/*
 * =============================================================================
 * 클래스명 : AnswerVerificationFactExtractor
 * =============================================================================
 * 목적
 *  - 검증 완료 Evidence의 구조화 결과를 Answer Verification용 Fact 후보로 변환한다.
 *  - Map/List 중첩 구조를 도메인 중립적인 factReference + scalar 값으로 평탄화한다.
 *  - 특정 회사, 업무, 필드명을 공통 검증 계층에 하드코딩하지 않는다.
 *  - 지원하지 않는 값 타입을 임의 문자열로 변환하지 않고 검증 경계에서 차단한다.
 */

package com.example.enterpriseai.service.evaluation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AnswerVerificationFactExtractor {

    // 검증 완료 Result에서 비교 가능한 scalar Fact 후보를 추출한다.
    public List<FactCandidate> extract(Map<String, Object> validatedResult) {
        if (validatedResult == null) {
            throw new IllegalArgumentException(
                    "Answer Verification 대상 검증 결과가 없습니다."
            );
        }

        List<FactCandidate> facts = new ArrayList<>();

        for (Map.Entry<String, Object> entry : validatedResult.entrySet()) {
            String key = validateKey(entry.getKey());
            extractValue(key, entry.getValue(), facts);
        }

        return List.copyOf(facts);
    }

    private void extractValue(
            String factReference,
            Object value,
            List<FactCandidate> facts
    ) {
        if (value == null) {
            return;
        }

        if (value instanceof Map<?, ?> map) {
            extractMap(factReference, map, facts);
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            extractIterable(factReference, iterable, facts);
            return;
        }

        if (isSupportedScalar(value)) {
            facts.add(
                    new FactCandidate(
                            factReference,
                            normalizeScalarValue(value)
                    )
            );
            return;
        }

        throw new IllegalArgumentException(
                "Answer Verification에서 지원하지 않는 값 타입입니다. factReference="
                        + factReference
                        + ", type="
                        + value.getClass().getName()
        );
    }

    private void extractMap(
            String parentReference,
            Map<?, ?> map,
            List<FactCandidate> facts
    ) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException(
                        "Answer Verification Map Key는 문자열이어야 합니다. factReference="
                                + parentReference
                );
            }

            String validatedKey = validateKey(key);

            extractValue(
                    parentReference + "." + validatedKey,
                    entry.getValue(),
                    facts
            );
        }
    }

    private void extractIterable(
            String parentReference,
            Iterable<?> iterable,
            List<FactCandidate> facts
    ) {
        int index = 0;

        for (Object item : iterable) {
            extractValue(
                    parentReference + "[" + index + "]",
                    item,
                    facts
            );

            index++;
        }
    }

    private String validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Answer Verification Fact Key가 없습니다."
            );
        }

        return key.trim();
    }

    private boolean isSupportedScalar(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof TemporalAccessor;
    }

    private String normalizeScalarValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }

        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        return String.valueOf(value).trim();
    }

    /*
     * Verifier 실행 중에만 사용하는 Fact 후보이다.
     * Evaluation 저장 모델에는 실제 comparableValue를 그대로 저장하지 않는다.
     */
    public record FactCandidate(
            String factReference,
            String comparableValue
    ) {

        public FactCandidate {
            if (factReference == null || factReference.isBlank()) {
                throw new IllegalArgumentException(
                        "Fact 참조값이 없습니다."
                );
            }

            if (comparableValue == null || comparableValue.isBlank()) {
                throw new IllegalArgumentException(
                        "Fact 비교값이 없습니다."
                );
            }

            factReference = factReference.trim();
        }
    }
}