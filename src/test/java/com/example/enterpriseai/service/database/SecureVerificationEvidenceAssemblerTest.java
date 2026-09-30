/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidenceAssemblerTest
 * =============================================================================
 * 목적
 *  - Secure Verification Evidence 조립 시 실제 Query 저장 정책을 검증한다.
 *  - allowActualQuery=false이면 실제 SQL 원문이 제거되는지 확인한다.
 *  - Query Reference는 유지되어 재검증 연결이 가능한지 확인한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.SecureVerificationEvidence;
import com.example.enterpriseai.dto.SecureVerificationEvidencePolicy;
import com.example.enterpriseai.service.security.SecureVerificationEvidenceSanitizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecureVerificationEvidenceAssemblerTest {

    private final SecureVerificationEvidenceAssembler assembler =
            new SecureVerificationEvidenceAssembler(
                    new SecureVerificationEvidenceSanitizer()
            );

    @Test
    void assembleRemovesActualQueryWhenPolicyDoesNotAllowIt() {
        SecureVerificationEvidencePolicy policy =
                new SecureVerificationEvidencePolicy(
                        false,
                        Set.of(),
                        Set.of(),
                        SecureVerificationEvidencePolicy.ResultStorageMode.SNAPSHOT,
                        10,
                        5000,
                        5000,
                        30
                );

        SecureVerificationEvidence.QueryEvidence queryEvidence =
                new SecureVerificationEvidence.QueryEvidence(
                        "SELECT column_a FROM dbo.allowed_table",
                        "execution-1234",
                        "v1",
                        "query-hash-1234"
                );

        Map<String, Object> validatedResult =
                Map.of(
                        "rows",
                        java.util.List.of(
                                Map.of(
                                        "fieldA",
                                        "valueA"
                                )
                        )
                );

        SecureVerificationEvidence evidence =
                assembler.assemble(
                        "현재 데이터를 조회해줘.",
                        "enterprise-ai",
                        "SAFE_QUERY",
                        "SAFE_TEXT_TO_SQL",
                        queryEvidence,
                        Map.of(),
                        validatedResult,
                        1,
                        null,
                        "result-hash-1234",
                        "검증 완료 조회 결과는 1건입니다.",
                        "조회 결과는 1건입니다.",
                        SecureVerificationEvidence.VerificationStatus.MATCH,
                        LocalDateTime.of(
                                2026,
                                9,
                                30,
                                12,
                                0
                        ),
                        policy
                );

        assertNull(
                evidence.queryEvidence().actualQuery()
        );

        assertEquals(
                "execution-1234",
                evidence.queryEvidence().queryReference()
        );

        assertEquals(
                "query-hash-1234",
                evidence.queryEvidence().queryHash()
        );

        assertEquals(
                1,
                evidence.resultCount()
        );

        assertEquals(
                SecureVerificationEvidence.VerificationStatus.MATCH,
                evidence.verificationStatus()
        );
    }
}