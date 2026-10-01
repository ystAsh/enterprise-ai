/*
 * =============================================================================
 * 클래스명 : DatabaseQueryExecutionContext
 * =============================================================================
 * 목적
 *  - 검증 완료 Database 실행 결과와 내부 검증 정보를 함께 전달한다.
 *  - Secure Verification Evidence 조립에 필요한 최소 실행 정보만 유지한다.
 *  - 실제 실행에 사용된 검증 완료 Parameter를 공통 Map 형태로 유지한다.
 *  - 기존 시스템 또는 Safe Query 실행 경계에서 확보한 Query Evidence를 유지한다.
 *  - 일반 사용자용 DatabaseQueryResult에 내부 검증 정보를 섞지 않는다.
 *  - API 응답에 직접 노출하지 않는 서버 내부 전달 객체이다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DatabaseQueryExecutionContext(
        DatabaseQueryResult queryResult,
        Map<String, Object> executedParameters,
        String source,
        String queryKey,
        String executionType,
        SecureVerificationEvidence.QueryEvidence queryEvidence,
        LocalDateTime executedAt
) {

    public DatabaseQueryExecutionContext {
        if (queryResult == null) {
            throw new IllegalArgumentException(
                    "검증 완료 Database Query Result는 필수입니다."
            );
        }

        executedParameters = executedParameters == null
                ? Map.of()
                : Map.copyOf(executedParameters);

        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException(
                    "Database 실행 Source는 필수입니다."
            );
        }

        if (queryKey == null || queryKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Database Query Key는 필수입니다."
            );
        }

        if (executionType == null || executionType.isBlank()) {
            throw new IllegalArgumentException(
                    "Database 실행 방식은 필수입니다."
            );
        }

        if (queryEvidence == null) {
            throw new IllegalArgumentException(
                    "Query Evidence 상태 정보는 필수입니다."
            );
        }

        if (executedAt == null) {
            throw new IllegalArgumentException(
                    "Database 실행 시각은 필수입니다."
            );
        }
    }
}