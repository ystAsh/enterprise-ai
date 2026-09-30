/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidenceSanitizerTest
 * =============================================================================
 * 목적
 *  - Secure Verification Evidence 저장 전 보안 정제 정책을 검증한다.
 *  - 허용되지 않은 Parameter가 저장되지 않는지 확인한다.
 *  - 민감 Parameter가 원문 대신 마스킹되는지 확인한다.
 *  - Snapshot 허용 범위를 초과한 대량 Result가 저장되지 않는지 확인한다.
 */

package com.example.enterpriseai.service.security;

import com.example.enterpriseai.dto.SecureVerificationEvidencePolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecureVerificationEvidenceSanitizerTest {

    private final SecureVerificationEvidenceSanitizer sanitizer =
            new SecureVerificationEvidenceSanitizer();

    @Test
    void sanitizeParametersAllowsOnlyRegisteredParametersAndMasksSensitiveValues() {
        SecureVerificationEvidencePolicy policy =
                new SecureVerificationEvidencePolicy(
                        false,
                        Set.of(
                                "keyword",
                                "organizationId"
                        ),
                        Set.of(
                                "organizationId"
                        ),
                        SecureVerificationEvidencePolicy.ResultStorageMode.SNAPSHOT,
                        20,
                        5000,
                        5000,
                        30
                );

        Map<String, Object> parameters =
                Map.of(
                        "keyword", "AC",
                        "organizationId", 100L,
                        "password", "secret"
                );

        Map<String, Object> sanitized =
                sanitizer.sanitizeParameters(
                        parameters,
                        policy
                );

        assertEquals(
                "AC",
                sanitized.get("keyword")
        );

        assertEquals(
                "[MASKED]",
                sanitized.get("organizationId")
        );

        assertFalse(
                sanitized.containsKey("password")
        );

        assertEquals(
                2,
                sanitized.size()
        );
    }

    @Test
    void sanitizeResultRejectsSnapshotWhenResultCountExceedsPolicyLimit() {
        SecureVerificationEvidencePolicy policy =
                new SecureVerificationEvidencePolicy(
                        false,
                        Set.of(),
                        Set.of(),
                        SecureVerificationEvidencePolicy.ResultStorageMode.SNAPSHOT,
                        2,
                        5000,
                        5000,
                        30
                );

        Map<String, Object> validatedResult =
                Map.of(
                        "rows",
                        java.util.List.of(
                                Map.of("fieldA", "A"),
                                Map.of("fieldA", "B"),
                                Map.of("fieldA", "C")
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sanitizer.sanitizeResult(
                                validatedResult,
                                3,
                                null,
                                "result-hash",
                                policy
                        )
                );

        assertEquals(
                "Secure Verification Evidence Snapshot 최대 행 수를 초과했습니다.",
                exception.getMessage()
        );
    }
}