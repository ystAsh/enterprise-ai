/*
 * =============================================================================
 * 클래스명 : SecureVerificationEvidenceAssembler
 * =============================================================================
 * 목적
 *  - Query 실행 및 AI 답변 검증 정보를 Secure Verification Evidence로 조립한다.
 *  - Parameter, Result, LLM Context, Answer는 저장 정책과 Sanitizer를 통과시킨다.
 *  - 실제 Query는 정책에서 허용한 경우에만 Evidence에 포함한다.
 *  - 기존 시스템이 Query Evidence를 제공하지 않는 경우 그 상태를 그대로 유지한다.
 *  - 일반 Audit 및 일반 사용자 Evidence와 분리된 내부 검증 구조를 생성한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.SecureVerificationEvidence;
import com.example.enterpriseai.dto.SecureVerificationEvidencePolicy;
import com.example.enterpriseai.service.security.SecureVerificationEvidenceSanitizer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class SecureVerificationEvidenceAssembler {

    private final SecureVerificationEvidenceSanitizer sanitizer;

    public SecureVerificationEvidenceAssembler(
            SecureVerificationEvidenceSanitizer sanitizer
    ) {
        this.sanitizer = sanitizer;
    }

    // 정책을 적용하여 개발자/관리자 재검증용 Evidence를 생성한다.
    public SecureVerificationEvidence assemble(
            String question,
            String source,
            String queryKey,
            String executionType,
            SecureVerificationEvidence.QueryEvidence queryEvidence,
            Map<String, Object> parameters,
            Map<String, Object> validatedResult,
            long resultCount,
            String resultReference,
            String resultHash,
            String llmContext,
            String finalAnswer,
            SecureVerificationEvidence.VerificationStatus verificationStatus,
            LocalDateTime executedAt,
            SecureVerificationEvidencePolicy policy
    ) {
        if (policy == null) {
            throw new IllegalArgumentException(
                    "Secure Verification Evidence Policy는 필수입니다."
            );
        }

        SecureVerificationEvidence.QueryEvidence sanitizedQueryEvidence =
                sanitizeQueryEvidence(
                        queryEvidence,
                        policy
                );

        Map<String, Object> sanitizedParameters =
                sanitizer.sanitizeParameters(
                        parameters,
                        policy
                );

        SecureVerificationEvidenceSanitizer.SanitizedResult sanitizedResult =
                sanitizer.sanitizeResult(
                        validatedResult,
                        resultCount,
                        resultReference,
                        resultHash,
                        policy
                );

        String sanitizedContext =
                sanitizer.sanitizeContext(
                        llmContext,
                        policy
                );

        String sanitizedAnswer =
                sanitizer.sanitizeAnswer(
                        finalAnswer,
                        policy
                );

        return new SecureVerificationEvidence(
                question,
                source,
                queryKey,
                executionType,
                sanitizedQueryEvidence,
                sanitizedParameters,
                sanitizedResult.storedResult(),
                resultCount,
                sanitizedResult.resultReference(),
                sanitizedResult.resultHash(),
                sanitizedContext,
                sanitizedAnswer,
                verificationStatus,
                executedAt
        );
    }

    /*
     * 실제 Query 원문은 정책이 허용한 경우에만 유지한다.
     * 원본 시스템이 Query Reference를 제공하지 않아도 임의로 생성하지 않는다.
     */
    private SecureVerificationEvidence.QueryEvidence sanitizeQueryEvidence(
            SecureVerificationEvidence.QueryEvidence queryEvidence,
            SecureVerificationEvidencePolicy policy
    ) {
        if (queryEvidence == null) {
            throw new IllegalArgumentException(
                    "Query Evidence 상태 정보는 필수입니다."
            );
        }

        if (policy.allowActualQuery()) {
            return queryEvidence;
        }

        return new SecureVerificationEvidence.QueryEvidence(
                null,
                queryEvidence.queryReference(),
                queryEvidence.queryVersion(),
                queryEvidence.queryHash()
        );
    }
}