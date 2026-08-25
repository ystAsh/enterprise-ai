/*
 * =============================================================================
 * 클래스명 : DatabaseQueryExecutionService
 * =============================================================================
 * 목적
 *  - Database RAG의 공통 Query 실행 흐름을 관리한다.
 *  - 서버에 등록된 Query만 실행하고 실행 전/후 보안 검증을 강제한다.
 *  - 검증 완료 결과만 DatabaseQueryResult로 변환한다.
 *  - Query 실행 및 검증 상태를 내부 Audit Log에 기록한다.
 *  - 특정 회사, 업무, 테이블, Repository, Mapper에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryDefinition;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.security.DatabaseQueryValidator;
import com.example.enterpriseai.service.security.DatabaseResultValidator;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DatabaseQueryExecutionService {

    private static final String VALIDATION_PASSED =
            "PASSED";

    private static final String VALIDATION_FAILED =
            "FAILED";

    private static final String VALIDATION_SKIPPED =
            "SKIPPED";

    private final DatabaseQueryDefinitionRegistry definitionRegistry;
    private final DatabaseQueryValidator queryValidator;
    private final DatabaseQueryExecutorResolver executorResolver;
    private final DatabaseResultValidator resultValidator;
    private final AiQueryAuditLogService auditLogService;

    public DatabaseQueryExecutionService(
            DatabaseQueryDefinitionRegistry definitionRegistry,
            DatabaseQueryValidator queryValidator,
            DatabaseQueryExecutorResolver executorResolver,
            DatabaseResultValidator resultValidator,
            AiQueryAuditLogService auditLogService
    ) {
        this.definitionRegistry = definitionRegistry;
        this.queryValidator = queryValidator;
        this.executorResolver = executorResolver;
        this.resultValidator = resultValidator;
        this.auditLogService = auditLogService;
    }

    /*
     * 서버에 등록된 Query를 실행하고
     * 검증 완료 결과만 반환한다.
     *
     * queryKey는 실행 요청의 식별값일 뿐이며
     * 실제 실행 정보와 보안 정책은 서버 Registry에서 가져온다.
     */
    public DatabaseQueryResult execute(
            String queryKey,
            CurrentUser currentUser
    ) {

        validateCurrentUser(
                currentUser
        );

        /*
         * 등록되지 않은 queryKey는 여기서 즉시 차단된다.
         *
         * 클라이언트나 LLM이 전달한 executionType,
         * ValidationPolicy 등을 신뢰하지 않는다.
         */
        DatabaseQueryDefinition definition =
                definitionRegistry.getRequired(
                        queryKey
                );

        long startedAt =
                System.nanoTime();

        Map<String, Object> rawResult;

        /*
         * =============================================================================
         * 1. 조회 전 검증 + Query 실행
         * =============================================================================
         */
        try {

            queryValidator.validate(
                    definition,
                    definition.executionPolicy()
            );

            DatabaseQueryExecutor executor =
                    executorResolver.resolve(
                            definition
                    );

            rawResult =
                    executor.execute(
                            definition,
                            currentUser
                    );

        } catch (RuntimeException e) {

            safeAuditFailure(
                    definition,
                    null,
                    VALIDATION_SKIPPED,
                    startedAt,
                    e
            );

            throw e;
        }

        /*
         * =============================================================================
         * 2. 조회 결과 검증
         * =============================================================================
         */
        Map<String, Object> validatedResult;

        try {

            validatedResult =
                    resultValidator.validate(
                            rawResult,
                            definition.validationPolicy()
                    );

        } catch (RuntimeException e) {

            safeAuditFailure(
                    definition,
                    null,
                    VALIDATION_FAILED,
                    startedAt,
                    e
            );

            throw e;
        }

        /*
         * =============================================================================
         * 3. 검증 완료 결과 생성
         * =============================================================================
         */
        DatabaseQueryResult.Evidence evidence =
                new DatabaseQueryResult.Evidence(
                        definition.source(),
                        definition.queryKey(),
                        definition.queryName(),
                        definition.executionType(),
                        true
                );

        DatabaseQueryResult queryResult =
                new DatabaseQueryResult(
                        definition.queryType(),
                        validatedResult,
                        evidence
                );

        /*
         * =============================================================================
         * 4. 성공 Audit Log
         * =============================================================================
         *
         * 공통 실행 계층은 실제 SQL을 알지 못한다.
         * 따라서 parameterizedSql은 null로 저장한다.
         *
         * JdbcClient / Query Builder의 실제 parameterized SQL 기록은
         * 해당 실행 계층에서 별도로 연결할 수 있다.
         */
        auditLogService.save(
                definition.queryType(),
                definition.queryKey(),
                definition.executionType(),
                null,
                null,
                VALIDATION_PASSED,
                true,
                elapsedMs(startedAt)
        );

        return queryResult;
    }

    /*
     * Spring Security에서 생성한 CurrentUser가 반드시 필요하다.
     *
     * 인증 정보가 없는 상태에서는 DB Query 실행을 시작하지 않는다.
     */
    private void validateCurrentUser(
            CurrentUser currentUser
    ) {

        if (currentUser == null) {
            throw new SecurityException(
                    "인증된 사용자가 없습니다."
            );
        }
    }

    /*
     * 실패한 Query 실행/검증을 Audit Log에 기록한다.
     *
     * Audit 저장 실패가 원래 Query 예외를 덮어쓰지 않도록
     * suppressed exception으로 추가한다.
     */
    private void safeAuditFailure(
            DatabaseQueryDefinition definition,
            Long resultCount,
            String validationStatus,
            long startedAt,
            RuntimeException originalException
    ) {

        try {

            auditLogService.save(
                    definition.queryType(),
                    definition.queryKey(),
                    definition.executionType(),
                    null,
                    resultCount,
                    validationStatus,
                    false,
                    elapsedMs(startedAt)
            );

        } catch (RuntimeException auditException) {

            originalException.addSuppressed(
                    auditException
            );
        }
    }

    /*
     * Query 실행 시작 이후 경과 시간을 millisecond로 계산한다.
     */
    private long elapsedMs(
            long startedAt
    ) {

        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt
        );
    }
}