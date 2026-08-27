/*
 * =============================================================================
 * 클래스명 : DatabaseQueryParameterResolver
 * =============================================================================
 * 목적
 *  - 서버에서 확정된 Query Definition에 대해 실행 파라미터 후보를 생성하는
 *    공통 확장점이다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 *  - 생성된 후보는 검증 전 값이며 직접 실행할 수 없다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;

public interface DatabaseQueryParameterResolver {

    /*
     * 현재 Resolver가 해당 Query Definition을 처리할 수 있는지 확인한다.
     */
    boolean supports(
            DatabaseQueryDefinition definition
    );

    /*
     * 사용자 질문에서 Query 실행 파라미터 후보를 생성한다.
     */
    DatabaseQueryParameterCandidate resolve(
            String question,
            DatabaseQueryDefinition definition
    );
}