/*
 * =============================================================================
 * 클래스명 : DatabaseQueryParameterValidator
 * =============================================================================
 * 목적
 *  - 검증 전 Query 파라미터 후보를 Java 정책으로 검증한다.
 *  - 검증에 성공한 경우에만 실행 가능한 DatabaseQueryParameters로 변환한다.
 *  - LLM/클라이언트가 생성한 값을 신뢰하지 않는다.
 *  - 실제 Query 실행은 담당하지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;
import com.example.enterpriseai.dto.DatabaseQueryParameters;

public interface DatabaseQueryParameterValidator {

    DatabaseQueryParameters validate(
            DatabaseQueryDefinition definition,
            DatabaseQueryParameterCandidate candidate
    );
}