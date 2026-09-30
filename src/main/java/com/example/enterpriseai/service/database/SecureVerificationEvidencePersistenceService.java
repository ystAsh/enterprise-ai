/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidencePersistenceService
 * =============================================================================
 * 목적
 *  - 검증 및 정제가 완료된 Secure Verification Evidence를 MSSQL에 저장한다.
 *  - Parameter와 Result Map을 JSON 문자열로 직렬화한다.
 *  - Evidence Policy의 보관 기간을 기준으로 expiresAt을 계산한다.
 *  - 저장 정책, Parameter 마스킹, Result 제한 등의 보안 판단은 수행하지 않는다.
 *  - 일반 Audit 저장 Transaction과 독립된 Secure Evidence 저장 Transaction을 사용한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.SecureVerificationEvidence;
import com.example.enterpriseai.dto.SecureVerificationEvidencePolicy;
import com.example.enterpriseai.entity.SecureVerificationEvidenceEntity;
import com.example.enterpriseai.repository.SecureVerificationEvidenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SecureVerificationEvidencePersistenceService {

    private final SecureVerificationEvidenceRepository repository;
    private final ObjectMapper objectMapper;

    public SecureVerificationEvidencePersistenceService(
            SecureVerificationEvidenceRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // 검증 완료 Evidence를 별도 Transaction으로 영속화한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long save(
            SecureVerificationEvidence evidence,
            SecureVerificationEvidencePolicy policy
    ) {
        if (evidence == null) {
            throw new IllegalArgumentException(
                    "Secure Verification Evidence는 필수입니다."
            );
        }

        if (policy == null) {
            throw new IllegalArgumentException(
                    "Secure Verification Evidence Policy는 필수입니다."
            );
        }

        LocalDateTime expiresAt =
                evidence.executedAt()
                        .plusDays(policy.retentionDays());

        SecureVerificationEvidenceEntity entity =
                SecureVerificationEvidenceEntity.create(
                        evidence.question(),
                        evidence.source(),
                        evidence.queryKey(),
                        evidence.executionType(),
                        evidence.queryEvidence().actualQuery(),
                        evidence.queryEvidence().queryReference(),
                        evidence.queryEvidence().queryVersion(),
                        evidence.queryEvidence().queryHash(),
                        toJsonOrNull(evidence.storedParameters()),
                        toJsonOrNull(evidence.validatedResult()),
                        evidence.resultCount(),
                        evidence.resultReference(),
                        evidence.resultHash(),
                        evidence.llmContext(),
                        evidence.finalAnswer(),
                        evidence.verificationStatus().name(),
                        evidence.executedAt(),
                        expiresAt
                );

        SecureVerificationEvidenceEntity saved =
                repository.save(entity);

        return saved.getVerificationEvidenceId();
    }

    // 빈 Map은 DB에 NULL로 저장하고 값이 있을 때만 JSON으로 변환한다.
    private String toJsonOrNull(
            Map<String, Object> value
    ) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Secure Verification Evidence JSON 변환에 실패했습니다.",
                    e
            );
        }
    }
}