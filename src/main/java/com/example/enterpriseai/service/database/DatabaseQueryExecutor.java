/*
 * =============================================================================
 * 클래스명 : DatabaseQueryExecutor
 * =============================================================================
 * 목적
 *  - Database RAG에서 검증된 Query를 실행하기 위한 공통 확장 지점을 제공한다.
 *  - 특정 회사, 업무, 테이블, Repository, Mapper에 종속되지 않는다.
 *  - 실제 실행 구현은 기존 시스템의 JPA / MyBatis / Query Builder가 담당한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.security.CurrentUser;

import java.util.Map;

public interface DatabaseQueryExecutor {

    /*
     * 이 Executor가 해당 Query Definition을 실행할 수 있는지 확인한다.
     *
     * queryKey나 executionType 등을 기준으로
     * 실제 구현체가 자신의 처리 가능 여부를 판단한다.
     */
    boolean supports(
            DatabaseQueryDefinition definition
    );

    /*
     * 검증 완료된 Query Definition을 실행한다.
     *
     * CurrentUser는 Spring Security가 생성한 서버 권한 정보만 사용한다.
     * 브라우저나 LLM에서 전달받은 권한 값을 사용하지 않는다.
     *
     * 반환값은 아직 LLM에 전달할 수 있는 결과가 아니다.
     * 반드시 DatabaseResultValidator 검증을 거쳐야 한다.
     */
    Map<String, Object> execute(
            DatabaseQueryDefinition definition,
            CurrentUser currentUser
    );
}