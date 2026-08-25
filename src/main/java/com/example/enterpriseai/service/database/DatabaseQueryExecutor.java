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
import com.example.enterpriseai.dto.DatabaseQueryParameters;
import com.example.enterpriseai.security.CurrentUser;

import java.util.Map;

public interface DatabaseQueryExecutor {

    /*
     * 이 Executor가 해당 Query Definition을 실행할 수 있는지 확인한다.
     *
     * 서버에 등록된 Definition의 source, executionType 등을 기준으로
     * 실제 구현체가 자신의 처리 가능 여부를 판단한다.
     */
    boolean supports(
            DatabaseQueryDefinition definition
    );

    /*
     * 검증 완료된 Query Definition과 실행 파라미터를 이용해 Query를 실행한다.
     *
     * definition:
     *  - 서버가 허용한 Query 정의
     *
     * parameters:
     *  - 현재 질문에서 추출하고 검증한 실행 조건
     *
     * currentUser:
     *  - Spring Security가 생성한 서버 권한 정보
     *
     * 반환값은 아직 LLM에 전달할 수 있는 결과가 아니다.
     * 반드시 DatabaseResultValidator 검증을 거쳐야 한다.
     */
    Map<String, Object> execute(
            DatabaseQueryDefinition definition,
            DatabaseQueryParameters parameters,
            CurrentUser currentUser
    );
}