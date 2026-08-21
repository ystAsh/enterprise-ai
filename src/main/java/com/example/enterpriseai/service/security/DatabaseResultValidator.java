/*
 * =============================================================================
 * 클래스명 : DatabaseResultValidator
 * =============================================================================
 * 목적
 *  - Database RAG에서 조회된 업무 데이터 결과를 Java에서 검증한다.
 *  - 검증되지 않은 DB 결과가 Gemini Context로 전달되지 않도록 차단한다.
 */

package com.example.enterpriseai.service.security;

import org.springframework.stereotype.Service;

@Service
public class DatabaseResultValidator {

    /*
     * 직원 수 조회 결과를 검증한다.
     *
     * 현재 질문은 "우리 부서 직원 수"이므로
     * 음수 여부와 비정상적인 값만 최소 검증한다.
     */
    public long validateEmployeeCount(
            long employeeCount
    ) {

        if (employeeCount < 0) {
            throw new IllegalStateException(
                    "직원 수 조회 결과가 올바르지 않습니다."
            );
        }

        return employeeCount;
    }
}