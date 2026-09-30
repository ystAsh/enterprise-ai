/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidencePersistenceServiceTest
 * =============================================================================
 * 목적
 *  - Secure Verification Evidence 영속화 전 Entity 변환을 검증한다.
 *  - Parameter/Result Map이 JSON으로 정상 직렬화되는지 확인한다.
 *  - Evidence Policy의 retentionDays가 expiresAt에 정확히 반영되는지 확인한다.
 *  - 실제 DB 없이 Repository 전달값을 deterministic하게 검증한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.SecureVerificationEvidence;
import com.example.enterpriseai.dto.SecureVerificationEvidencePolicy;
import com.example.enterpriseai.entity.SecureVerificationEvidenceEntity;
import com.example.enterpriseai.repository.SecureVerificationEvidenceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecureVerificationEvidencePersistenceServiceTest {

    private final SecureVerificationEvidenceRepository repository =
            mock(SecureVerificationEvidenceRepository.class);

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final SecureVerificationEvidencePersistenceService service =
            new SecureVerificationEvidencePersistenceService(
                    repository,
                    objectMapper
            );

    @Test
    void saveConvertsEvidenceToEntityAndAppliesRetentionPolicy() throws Exception {
        LocalDateTime executedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        30,
                        13,
                        30
                );

        SecureVerificationEvidence evidence =
                new SecureVerificationEvidence(
                        "현재 데이터를 조회해줘.",
                        "enterprise-ai",
                        "SAFE_QUERY",
                        "SAFE_TEXT_TO_SQL",
                        new SecureVerificationEvidence.QueryEvidence(
                                null,
                                "execution-1234",
                                "v1",
                                "query-hash-1234"
                        ),
                        Map.of(
                                "keyword", "AC",
                                "organizationId", "[MASKED]"
                        ),
                        Map.of(
                                "rows",
                                List.of(
                                        Map.of(
                                                "fieldA",
                                                "valueA"
                                        )
                                )
                        ),
                        1,
                        null,
                        "result-hash-1234",
                        "검증 완료 조회 결과는 1건입니다.",
                        "조회 결과는 1건입니다.",
                        SecureVerificationEvidence.VerificationStatus.MATCH,
                        executedAt
                );

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
                        10,
                        5000,
                        5000,
                        30
                );

        SecureVerificationEvidenceEntity savedEntity =
                mock(SecureVerificationEvidenceEntity.class);

        when(savedEntity.getVerificationEvidenceId())
                .thenReturn(101L);

        when(repository.save(any(SecureVerificationEvidenceEntity.class)))
                .thenReturn(savedEntity);

        Long evidenceId =
                service.save(
                        evidence,
                        policy
                );

        ArgumentCaptor<SecureVerificationEvidenceEntity> entityCaptor =
                ArgumentCaptor.forClass(
                        SecureVerificationEvidenceEntity.class
                );

        verify(repository).save(
                entityCaptor.capture()
        );

        SecureVerificationEvidenceEntity entity =
                entityCaptor.getValue();

        assertEquals(
                101L,
                evidenceId
        );

        assertEquals(
                "현재 데이터를 조회해줘.",
                entity.getQuestion()
        );

        assertEquals(
                "enterprise-ai",
                entity.getSource()
        );

        assertEquals(
                "SAFE_QUERY",
                entity.getQueryKey()
        );

        assertNull(
                entity.getActualQuery()
        );

        assertEquals(
                "execution-1234",
                entity.getQueryReference()
        );

        assertEquals(
                1,
                entity.getResultCount()
        );

        assertEquals(
                "MATCH",
                entity.getVerificationStatus()
        );

        assertEquals(
                executedAt,
                entity.getExecutedAt()
        );

        assertEquals(
                executedAt.plusDays(30),
                entity.getExpiresAt()
        );

        JsonNode parametersJson =
                objectMapper.readTree(
                        entity.getStoredParametersJson()
                );

        assertEquals(
                "AC",
                parametersJson.get("keyword").asText()
        );

        assertEquals(
                "[MASKED]",
                parametersJson.get("organizationId").asText()
        );

        JsonNode resultJson =
                objectMapper.readTree(
                        entity.getValidatedResultJson()
                );

        assertEquals(
                "valueA",
                resultJson
                        .get("rows")
                        .get(0)
                        .get("fieldA")
                        .asText()
        );
    }
}