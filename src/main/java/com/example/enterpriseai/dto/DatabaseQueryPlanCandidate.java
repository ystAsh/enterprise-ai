/*
 * =============================================================================
 * 클래스명 : DatabaseQueryPlanCandidate
 * =============================================================================
 * 목적
 *  - Safe Text-to-SQL에서 LLM이 생성한 구조화된 Query Plan 후보를 표현한다.
 *  - LLM이 생성한 값은 신뢰하지 않으며 Java 검증 전에는 실행에 사용할 수 없다.
 *  - SQL 문자열을 직접 생성하거나 보관하지 않는다.
 *  - 특정 회사, 업무, 테이블, 컬럼에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.List;
import java.util.Map;

public record DatabaseQueryPlanCandidate(
        List<String> selectFields,
        Map<String, Object> filters,
        List<String> groupByFields,
        List<String> orderByFields,
        Integer maxRows
) {
}