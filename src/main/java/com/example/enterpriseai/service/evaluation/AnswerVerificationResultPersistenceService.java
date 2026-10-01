/*
 * =============================================================================
 * 클래스명 : AnswerVerificationResultPersistenceService
 * =============================================================================
 * 목적
 *  - Java deterministic Answer Verification 결과를 MSSQL에 영속화한다.
 *  - Secure Verification Evidence ID와 평가 결과를 연결한다.
 *  - Fact Count, Match Rate, Verifier Version, 평가 시각을 저장한다.
 *  - Raw SQL, Raw Result, 실제 Parameter, LLM Context를 중복 저장하지 않는다.
 */

package com.example.enterpriseai.service.evaluation;

import com.example.enterpriseai.dto.AnswerVerificationResult;
import com.example.enterpriseai.entity.AnswerVerificationResultEntity;
import com.example.enterpriseai.repository.AnswerVerificationResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnswerVerificationResultPersistenceService {

    private final AnswerVerificationResultRepository repository;

    public AnswerVerificationResultPersistenceService(
            AnswerVerificationResultRepository repository
    ) {
        this.repository = repository;
    }

    // Secure Verification Evidence와 연결된 질문 단위 평가 결과를 저장한다.
    @Transactional
    public AnswerVerificationResultEntity save(
            Long verificationEvidenceId,
            AnswerVerificationResult result
    ) {
        if (verificationEvidenceId == null || verificationEvidenceId <= 0) {
            throw new IllegalArgumentException(
                    "Secure Verification Evidence ID가 올바르지 않습니다."
            );
        }

        if (result == null) {
            throw new IllegalArgumentException(
                    "Answer Verification 결과가 없습니다."
            );
        }

        AnswerVerificationResultEntity entity =
                AnswerVerificationResultEntity.create(
                        verificationEvidenceId,
                        result,
                        LocalDateTime.now()
                );

        return repository.save(entity);
    }
}