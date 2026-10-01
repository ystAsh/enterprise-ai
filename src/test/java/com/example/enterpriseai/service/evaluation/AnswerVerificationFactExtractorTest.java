/*
 * =============================================================================
 * 클래스명 : AnswerVerificationFactExtractorTest
 * =============================================================================
 * 목적
 *  - AnswerVerificationFactExtractor가 검증 완료 구조화 결과를
 *    도메인 중립적인 scalar Fact 후보로 정확히 변환하는지 검증한다.
 *  - 중첩 Map/List, 숫자 정규화, null 제외, 지원하지 않는 타입 차단을 확인한다.
 */

package com.example.enterpriseai.service.evaluation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnswerVerificationFactExtractorTest {

    private final AnswerVerificationFactExtractor extractor =
            new AnswerVerificationFactExtractor();

    @Test
    void extractsNestedScalarFacts() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fieldA", "value-a");
        row.put("fieldB", 3);

        Map<String, Object> validatedResult = new LinkedHashMap<>();
        validatedResult.put("items", List.of(row));

        List<AnswerVerificationFactExtractor.FactCandidate> facts =
                extractor.extract(validatedResult);

        assertEquals(
                List.of(
                        new AnswerVerificationFactExtractor.FactCandidate(
                                "items[0].fieldA",
                                "value-a"
                        ),
                        new AnswerVerificationFactExtractor.FactCandidate(
                                "items[0].fieldB",
                                "3"
                        )
                ),
                facts
        );
    }

    @Test
    void normalizesBigDecimalValue() {
        Map<String, Object> validatedResult =
                Map.of(
                        "amount",
                        new BigDecimal("1200.000")
                );

        List<AnswerVerificationFactExtractor.FactCandidate> facts =
                extractor.extract(validatedResult);

        assertEquals(
                List.of(
                        new AnswerVerificationFactExtractor.FactCandidate(
                                "amount",
                                "1200"
                        )
                ),
                facts
        );
    }

    @Test
    void ignoresNullValues() {
        Map<String, Object> validatedResult = new LinkedHashMap<>();
        validatedResult.put("fieldA", null);
        validatedResult.put("fieldB", "value-b");

        List<AnswerVerificationFactExtractor.FactCandidate> facts =
                extractor.extract(validatedResult);

        assertEquals(
                List.of(
                        new AnswerVerificationFactExtractor.FactCandidate(
                                "fieldB",
                                "value-b"
                        )
                ),
                facts
        );
    }

    @Test
    void rejectsUnsupportedValueType() {
        Map<String, Object> validatedResult =
                Map.of(
                        "unsupported",
                        new UnsupportedFixture("value")
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> extractor.extract(validatedResult)
                );

        assertEquals(
                "Answer Verification에서 지원하지 않는 값 타입입니다. factReference=unsupported, type="
                        + UnsupportedFixture.class.getName(),
                exception.getMessage()
        );
    }

    @Test
    void rejectsBlankMapKey() {
        Map<String, Object> validatedResult = new LinkedHashMap<>();
        validatedResult.put(" ", "value");

        assertThrows(
                IllegalArgumentException.class,
                () -> extractor.extract(validatedResult)
        );
    }

    private record UnsupportedFixture(String value) {
    }
}