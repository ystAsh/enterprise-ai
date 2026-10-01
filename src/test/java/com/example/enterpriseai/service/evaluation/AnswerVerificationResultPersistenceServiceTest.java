/*
 * =============================================================================
 * 클래스명 : AnswerVerificationResultPersistenceServiceTest
 * =============================================================================
 * 목적
 *  - AnswerVerificationResultPersistenceService가 평가 결과를
 *    ai_answer_verification_result 테이블에 정확히 저장하는지 검증한다.
 *  - Secure Verification Evidence ID 연결과 Fact Count / Match Rate 저장을 확인한다.
 *  - 비교 가능한 Fact가 없는 경우 matchRate가 NULL로 저장되는지 확인한다.
 */

package com.example.enterpriseai.service.evaluation;

import com.example.enterpriseai.dto.AnswerVerificationResult;
import com.example.enterpriseai.dto.AnswerVerificationResult.FactVerification;
import com.example.enterpriseai.dto.SecureVerificationEvidence.VerificationStatus;
import com.example.enterpriseai.entity.AnswerVerificationResultEntity;
import com.example.enterpriseai.repository.AnswerVerificationResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
class AnswerVerificationResultPersistenceServiceTest {

    @Autowired
    private AnswerVerificationResultPersistenceService persistenceService;

    @Autowired
    private AnswerVerificationResultRepository repository;

    @Test
    void savesMatchResult() {
        AnswerVerificationResult result =
                new AnswerVerificationResult(
                        VerificationStatus.MATCH,
                        List.of(
                                new FactVerification(
                                        "items[0].fieldA",
                                        VerificationStatus.MATCH
                                ),
                                new FactVerification(
                                        "items[0].fieldB",
                                        VerificationStatus.MATCH
                                )
                        ),
                        "deterministic-scalar-v1"
                );

        AnswerVerificationResultEntity saved =
                persistenceService.save(
                        2L,
                        result
                );

        assertNotNull(saved.getId());

        AnswerVerificationResultEntity found =
                repository.findById(saved.getId()).orElseThrow();

        assertEquals(2L, found.getVerificationEvidenceId());
        assertEquals(VerificationStatus.MATCH, found.getOverallStatus());
        assertEquals(2, found.getMatchedFactCount());
        assertEquals(0, found.getMismatchFactCount());
        assertEquals(0, found.getUnsupportedFactCount());
        assertEquals(0, found.getMissingFactCount());
        assertEquals(2, found.getEvaluatedFactCount());

        assertEquals(
                new BigDecimal("100.00"),
                found.getMatchRate()
        );

        assertEquals(
                "deterministic-scalar-v1",
                found.getVerifierVersion()
        );

        assertNotNull(found.getVerifiedAt());
    }

    @Test
    void savesNullMatchRateWhenNoComparableFactExists() {
        AnswerVerificationResult result =
                new AnswerVerificationResult(
                        VerificationStatus.UNSUPPORTED,
                        List.of(),
                        "deterministic-scalar-v1"
                );

        AnswerVerificationResultEntity saved =
                persistenceService.save(
                        2L,
                        result
                );

        AnswerVerificationResultEntity found =
                repository.findById(saved.getId()).orElseThrow();

        assertEquals(
                VerificationStatus.UNSUPPORTED,
                found.getOverallStatus()
        );

        assertEquals(0, found.getMatchedFactCount());
        assertEquals(0, found.getMismatchFactCount());
        assertEquals(0, found.getUnsupportedFactCount());
        assertEquals(0, found.getMissingFactCount());
        assertEquals(0, found.getEvaluatedFactCount());

        assertNull(found.getMatchRate());
    }
}