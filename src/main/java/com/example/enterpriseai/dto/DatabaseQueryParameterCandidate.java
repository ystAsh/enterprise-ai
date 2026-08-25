/*
 * =============================================================================
 * 클래스명 : DatabaseQueryParameterCandidate
 * =============================================================================
 * 목적
 *  - 사용자 질문에서 추출한 검증 전 Query 파라미터 후보를 전달한다.
 *  - 이 값은 신뢰하지 않으며 Java Validator 검증 전에는 실행에 사용할 수 없다.
 *  - SQL, 테이블명, Repository, 권한정보 등 서버 내부 실행 정보를 포함하지 않는다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record DatabaseQueryParameterCandidate(

        // 사용자 질문에서 추출한 검증 전 파라미터 후보
        Map<String, Object> values

) {

    public DatabaseQueryParameterCandidate {

        if (values == null) {
            values = Map.of();
        } else {
            values = Collections.unmodifiableMap(
                    new HashMap<>(values)
            );
        }
    }
}