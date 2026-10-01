/*
 * =============================================================================
 * 클래스명 : AnswerVerificationResultRepository
 * =============================================================================
 * 목적
 *  - Answer Verification 질문 단위 평가 결과의 JPA 영속화 경계를 제공한다.
 *  - Evaluation 결과를 Secure Verification Evidence와 ID 기준으로 연결해 조회할 수 있게 한다.
 *  - Secure Verification Evidence 원문이나 업무별 조회 로직을 직접 다루지 않는다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.AnswerVerificationResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerVerificationResultRepository
        extends JpaRepository<AnswerVerificationResultEntity, Long> {

    List<AnswerVerificationResultEntity>
    findByVerificationEvidenceIdOrderByVerifiedAtDesc(
            Long verificationEvidenceId
    );
}