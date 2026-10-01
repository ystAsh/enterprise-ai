/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidencePolicyProvider
 * =============================================================================
 * 목적
 *  - Secure Verification Evidence 저장 정책을 공통으로 생성한다.
 *  - 실제 실행 Parameter 이름은 유지하되 현재 단계에서는 모든 값을 마스킹한다.
 *  - 실제 Query는 제한된 Secure Verification Evidence에서만 저장 가능하게 한다.
 *  - Context/Answer 크기와 보관 기간을 설정값으로 관리한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.security;

import com.example.enterpriseai.dto.SecureVerificationEvidencePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SecureVerificationEvidencePolicyProvider {

    private final int maxContextLength;
    private final int maxAnswerLength;
    private final int retentionDays;

    public SecureVerificationEvidencePolicyProvider(
            @Value("${enterprise-ai.secure-verification.max-context-length:5000}")
            int maxContextLength,
            @Value("${enterprise-ai.secure-verification.max-answer-length:5000}")
            int maxAnswerLength,
            @Value("${enterprise-ai.secure-verification.retention-days:30}")
            int retentionDays
    ) {
        this.maxContextLength = maxContextLength;
        this.maxAnswerLength = maxAnswerLength;
        this.retentionDays = retentionDays;
    }

    // 실제 실행 Parameter 이름은 유지하되 값은 모두 마스킹하는 기본 정책을 생성한다.
    public SecureVerificationEvidencePolicy create(
            Set<String> executedParameterNames,
            SecureVerificationEvidencePolicy.ResultStorageMode resultStorageMode,
            int maxStoredResultRows
    ) {
        Set<String> parameterNames =
                executedParameterNames == null
                        ? Set.of()
                        : Set.copyOf(executedParameterNames);

        return new SecureVerificationEvidencePolicy(
                true,
                parameterNames,
                parameterNames,
                resultStorageMode,
                maxStoredResultRows,
                maxContextLength,
                maxAnswerLength,
                retentionDays
        );
    }
}