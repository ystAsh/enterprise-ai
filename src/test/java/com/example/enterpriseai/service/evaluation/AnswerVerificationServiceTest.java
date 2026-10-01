/*
 * =============================================================================
 * 클래스명 : AnswerVerificationServiceTest
 * =============================================================================
 * 목적
 *  - AnswerVerificationService의 deterministic 검증 결과를 확인한다.
 *  - MATCH / MISSING / UNSUPPORTED 판정을 검증한다.
 *  - Match Rate가 Fact Count를 기준으로 정확히 계산되는지 확인한다.
 *  - 업무별 필드명을 사용하지 않고 도메인 중립 fixture로 검증한다.
 */

package com.example.enterpriseai.service.evaluation;

import com.example.enterpriseai.dto.AnswerVerificationPolicy;
import com.example.enterpriseai.dto.AnswerVerificationResult;
import com.example.enterpriseai.dto.SecureVerificationEvidence.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnswerVerificationServiceTest {

    private AnswerVerificationService service;

    @BeforeEach
    void setUp() {
        service = new AnswerVerificationService(
                new AnswerVerificationFactExtractor()
        );
    }

    @Test
    void verifiesAllFactsAsMatch() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fieldA", "value-a");
        row.put("fieldB", 3);

        Map<String, Object> validatedResult = new LinkedHashMap<>();
        validatedResult.put("items", List.of(row));

        AnswerVerificationResult result =
                service.verify(
                        validatedResult,
                        "조회 결과는 value-a이고 값은 3입니다.",
                        new AnswerVerificationPolicy(
                                Set.of(
                                        "items[0].fieldA",
                                        "items[0].fieldB"
                                )
                        )
                );

        assertEquals(
                VerificationStatus.MATCH,
                result.overallStatus()
        );

        assertEquals(2, result.matchedFactCount());
        assertEquals(0, result.mismatchFactCount());
        assertEquals(0, result.unsupportedFactCount());
        assertEquals(0, result.missingFactCount());
        assertEquals(2, result.evaluatedFactCount());
        assertEquals(2, result.comparableFactCount());

        assertEquals(
                new BigDecimal("100.00"),
                result.matchRate()
        );
    }

    @Test
    void marksRequiredMissingFactAsMissing() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fieldA", "value-a");
        row.put("fieldB", 3);

        Map<String, Object> validatedResult = new LinkedHashMap<>();
        validatedResult.put("items", List.of(row));

        AnswerVerificationResult result =
                service.verify(
                        validatedResult,
                        "조회 결과는 value-a입니다.",
                        new AnswerVerificationPolicy(
                                Set.of(
                                        "items[0].fieldA",
                                        "items[0].fieldB"
                                )
                        )
                );

        assertEquals(
                VerificationStatus.MISSING,
                result.overallStatus()
        );

        assertEquals(1, result.matchedFactCount());
        assertEquals(0, result.mismatchFactCount());
        assertEquals(0, result.unsupportedFactCount());
        assertEquals(1, result.missingFactCount());
        assertEquals(2, result.evaluatedFactCount());
        assertEquals(1, result.comparableFactCount());

        assertEquals(
                new BigDecimal("100.00"),
                result.matchRate()
        );
    }

    @Test
    void doesNotMarkOptionalMissingFactAsMissing() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fieldA", "value-a");
        row.put("fieldB", 3);

        Map<String, Object> validatedResult = new LinkedHashMap<>();
        validatedResult.put("items", List.of(row));

        AnswerVerificationResult result =
                service.verify(
                        validatedResult,
                        "조회 결과는 value-a입니다.",
                        new AnswerVerificationPolicy(
                                Set.of("items[0].fieldA")
                        )
                );

        assertEquals(
                VerificationStatus.MATCH,
                result.overallStatus()
        );

        assertEquals(1, result.matchedFactCount());
        assertEquals(0, result.mismatchFactCount());
        assertEquals(0, result.unsupportedFactCount());
        assertEquals(0, result.missingFactCount());
        assertEquals(1, result.evaluatedFactCount());

        assertEquals(
                new BigDecimal("100.00"),
                result.matchRate()
        );
    }

    @Test
    void returnsUnsupportedWhenNoEvidenceFactIsGrounded() {
        Map<String, Object> validatedResult =
                Map.of(
                        "fieldA",
                        "value-a"
                );

        AnswerVerificationResult result =
                service.verify(
                        validatedResult,
                        "근거와 연결되지 않는 설명입니다.",
                        AnswerVerificationPolicy.noneRequired()
                );

        assertEquals(
                VerificationStatus.UNSUPPORTED,
                result.overallStatus()
        );

        assertEquals(0, result.matchedFactCount());
        assertEquals(0, result.mismatchFactCount());
        assertEquals(0, result.unsupportedFactCount());
        assertEquals(0, result.missingFactCount());
        assertEquals(0, result.evaluatedFactCount());
        assertEquals(0, result.comparableFactCount());
        assertNull(result.matchRate());
    }

    @Test
    void keepsVerifierVersion() {
        AnswerVerificationResult result =
                service.verify(
                        Map.of(
                                "fieldA",
                                "value-a"
                        ),
                        "value-a",
                        AnswerVerificationPolicy.noneRequired()
                );

        assertEquals(
                "deterministic-scalar-v1",
                result.verifierVersion()
        );
    }
}