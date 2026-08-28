/*
 * =============================================================================
 * 클래스명 : DatabaseQueryExecutor
 * =============================================================================
 * 목적
 *  - Database RAG에서 검증된 Query를 실행하기 위한 공통 확장 지점을 제공한다.
 *  - 실행 결과 데이터와 공통 건수 Metadata를 함께 반환한다.
 *  - 특정 회사, 업무, 테이블, Repository, Mapper에 종속되지 않는다.
 *  - 실제 실행 구현은 기존 시스템의 JPA / MyBatis / Query Builder 등이 담당한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryExecutionResult;
import com.example.enterpriseai.dto.DatabaseQueryParameters;
import com.example.enterpriseai.security.CurrentUser;

public interface DatabaseQueryExecutor {

    /*
     * 이 Executor가 해당 Query Definition을 실행할 수 있는지 확인한다.
     */
    boolean supports(
            DatabaseQueryDefinition definition
    );

    /*
     * 검증 완료된 Query Definition과 실행 파라미터로 Query를 실행한다.
     *
     * 반환 결과:
     *  - data: 아직 Result Validator 검증 전인 Raw Result
     *  - returnedCount: 이번 실행에서 실제 반환된 행 수
     *  - totalCount: 기존 조회 기능이 명확히 제공할 때만 전체 건수
     *
     * 반환 결과는 아직 LLM에 전달할 수 없다.
     * 반드시 DatabaseResultValidator 검증을 거쳐야 한다.
     */
    DatabaseQueryExecutionResult execute(
            DatabaseQueryDefinition definition,
            DatabaseQueryParameters parameters,
            CurrentUser currentUser
    );
}