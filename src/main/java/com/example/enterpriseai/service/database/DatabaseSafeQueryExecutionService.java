/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQueryExecutionService
 * =============================================================================
 * 목적
 *  - 검증 완료 DatabaseQueryPlan을 Safe Query로 변환하고 MSSQL에서 실행한다.
 *  - 실행 결과를 기존 DatabaseResultValidator로 검증한다.
 *  - 검증 완료 결과를 DatabaseQueryResult로 변환한다.
 *  - Secure Verification 연결이 필요한 경우 실제 실행 Parameter와 Query Evidence를 함께 반환한다.
 *  - Safe Text-to-SQL의 실행/결과 검증 경계를 하나의 공통 흐름으로 관리한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryExecutionContext;
import com.example.enterpriseai.dto.DatabaseQueryPlan;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.dto.DatabaseQueryResultMetadata;
import com.example.enterpriseai.dto.DatabaseSafeQuery;
import com.example.enterpriseai.dto.DatabaseSafeQueryPolicy;
import com.example.enterpriseai.dto.SecureVerificationEvidence;
import com.example.enterpriseai.service.security.DatabaseResultValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseSafeQueryExecutionService {

    private static final String ROWS_FIELD = "rows";

    private final DatabaseSafeQueryBuilder queryBuilder;
    private final DatabaseSafeQueryExecutor queryExecutor;
    private final DatabaseResultValidator resultValidator;

    public DatabaseSafeQueryExecutionService(
            DatabaseSafeQueryBuilder queryBuilder,
            DatabaseSafeQueryExecutor queryExecutor,
            DatabaseResultValidator resultValidator
    ) {
        this.queryBuilder = queryBuilder;
        this.queryExecutor = queryExecutor;
        this.resultValidator = resultValidator;
    }

    /*
     * 기존 호출부 호환용이다.
     * Secure Verification 연결 정보가 필요한 호출부는 executeWithContext()를 사용한다.
     */
    public DatabaseQueryResult execute(
            DatabaseQueryPlan plan,
            DatabaseSafeQueryPolicy policy
    ) {
        return executeWithContext(
                plan,
                policy
        ).queryResult();
    }

    // Safe Query를 실제 실행하고 검증 완료 결과와 내부 실행 Context를 함께 반환한다.
    public DatabaseQueryExecutionContext executeWithContext(
            DatabaseQueryPlan plan,
            DatabaseSafeQueryPolicy policy
    ) {
        if (plan == null) {
            throw new IllegalArgumentException(
                    "검증 완료 Query Plan이 없습니다."
            );
        }

        if (policy == null) {
            throw new IllegalArgumentException(
                    "Safe Query 정책이 없습니다."
            );
        }

        DatabaseSafeQuery safeQuery =
                queryBuilder.build(
                        plan,
                        policy
                );

        List<Map<String, Object>> rows =
                queryExecutor.execute(
                        safeQuery
                );

        Map<String, Object> rawResult =
                new LinkedHashMap<>();

        rawResult.put(
                ROWS_FIELD,
                rows
        );

        Map<String, Object> validatedResult =
                resultValidator.validate(
                        rawResult,
                        policy.validationPolicy()
                );

        DatabaseQueryResultMetadata metadata =
                DatabaseQueryResultMetadata.returned(
                        rows.size()
                );

        DatabaseQueryResult.Evidence evidence =
                new DatabaseQueryResult.Evidence(
                        policy.source(),
                        policy.queryKey(),
                        policy.queryName(),
                        policy.executionType(),
                        true
                );

        DatabaseQueryResult queryResult =
                new DatabaseQueryResult(
                        policy.queryType(),
                        validatedResult,
                        metadata,
                        evidence
                );

        // enterprise-ai가 실제 생성하고 Executor에 전달한 Safe Query를 Evidence로 유지한다.
        SecureVerificationEvidence.QueryEvidence queryEvidence =
                new SecureVerificationEvidence.QueryEvidence(
                        safeQuery.sql(),
                        null,
                        null,
                        null
                );

        return new DatabaseQueryExecutionContext(
                queryResult,
                safeQuery.parameters(),
                policy.source(),
                policy.queryKey(),
                policy.executionType(),
                queryEvidence,
                LocalDateTime.now()
        );
    }
}