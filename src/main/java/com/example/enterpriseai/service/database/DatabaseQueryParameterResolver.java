/*
 * =============================================================================
 * 클래스명 : DatabaseQueryParameterResolver
 * =============================================================================
 * 목적
 *  - 사용자 자연어 질문에서 서버가 확정한 Query Definition에 필요한
 *    Query 실행 파라미터 후보를 생성한다.
 *  - 생성된 후보는 신뢰하지 않으며 Java Validator 검증 전에는 실행하지 않는다.
 *  - Query 실행 가능 여부나 사용자 권한은 판단하지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryParameterCandidate;

public interface DatabaseQueryParameterResolver {

    DatabaseQueryParameterCandidate resolve(
            String question,
            DatabaseQueryDefinition definition
    );
}