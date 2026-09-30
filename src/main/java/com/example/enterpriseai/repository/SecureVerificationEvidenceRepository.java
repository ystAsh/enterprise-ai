/*
 * =============================================================================
 * 인터페이스명 : SecureVerificationEvidenceRepository
 * =============================================================================
 * 목적
 *  - Secure Verification Evidence Entity의 MSSQL 영속화를 담당한다.
 *  - 저장 정책이나 보안 판단은 수행하지 않는다.
 *  - Sanitizer와 Assembler를 통과한 Evidence만 상위 Service에서 전달받아 저장한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.SecureVerificationEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecureVerificationEvidenceRepository
        extends JpaRepository<SecureVerificationEvidenceEntity, Long> {
}