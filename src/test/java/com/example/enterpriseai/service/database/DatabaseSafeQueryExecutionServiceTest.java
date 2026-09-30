/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQueryExecutionServiceTest
 * =============================================================================
 * 목적
 *  - Safe Query 실행 결과가 기존 DatabaseResultValidator를 거쳐 검증되는지 확인한다.
 *  - 검증 완료 결과가 DatabaseQueryResult와 Evidence로 변환되는지 확인한다.
 *  - Safe Query에서 알 수 없는 totalCount를 임의 생성하지 않는지 검증한다.
 *  - 허용되지 않은 결과 필드는 LLM 전달 전에 차단되는지 확인한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryPlan;
import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.dto.DatabaseSafeQuery;
import com.example.enterpriseai.dto.DatabaseSafeQueryPolicy;
import com.example.enterpriseai.dto.DatabaseValidationPolicy;
import com.example.enterpriseai.service.security.DatabaseResultValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseSafeQueryExecutionServiceTest {

    private final DatabaseSafeQueryBuilder queryBuilder =
            mock(DatabaseSafeQueryBuilder.class);

    private final DatabaseSafeQueryExecutor queryExecutor =
            mock(DatabaseSafeQueryExecutor.class);

    private final DatabaseResultValidator resultValidator =
            new DatabaseResultValidator();

    private final DatabaseSafeQueryExecutionService executionService =
            new DatabaseSafeQueryExecutionService(
                    queryBuilder,
                    queryExecutor,
                    resultValidator
            );

    @Test
    void executeReturnsValidatedDatabaseQueryResult() {
        DatabaseQueryPlan plan = createPlan();
        DatabaseSafeQueryPolicy policy = createPolicy();

        DatabaseSafeQuery safeQuery =
                new DatabaseSafeQuery(
                        "SELECT TOP (10) [column_a] AS [fieldA] "
                                + "FROM [dbo].[allowed_table]",
                        Map.of(),
                        10
                );

        List<Map<String, Object>> rows =
                List.of(
                        Map.of("fieldA", "VALUE")
                );

        when(queryBuilder.build(plan, policy))
                .thenReturn(safeQuery);

        when(queryExecutor.execute(safeQuery))
                .thenReturn(rows);

        DatabaseQueryResult result =
                executionService.execute(
                        plan,
                        policy
                );

        assertEquals(
                "SAFE_QUERY",
                result.queryType()
        );

        assertEquals(
                rows,
                result.data().get("rows")
        );

        assertEquals(
                1,
                result.metadata().returnedCount()
        );

        assertNull(
                result.metadata().totalCount()
        );

        assertEquals(
                "mssql",
                result.evidence().source()
        );

        assertEquals(
                "safe-query-test",
                result.evidence().queryKey()
        );

        assertEquals(
                "Safe Query Test",
                result.evidence().queryName()
        );

        assertEquals(
                "SAFE_TEXT_TO_SQL",
                result.evidence().executionType()
        );

        assertEquals(
                true,
                result.evidence().validated()
        );

        verify(queryBuilder)
                .build(plan, policy);

        verify(queryExecutor)
                .execute(safeQuery);
    }

    @Test
    void executeRejectsResultContainingUnallowedField() {
        DatabaseQueryPlan plan = createPlan();
        DatabaseSafeQueryPolicy policy = createPolicy();

        DatabaseSafeQuery safeQuery =
                new DatabaseSafeQuery(
                        "SELECT TOP (10) [column_a] AS [fieldA] "
                                + "FROM [dbo].[allowed_table]",
                        Map.of(),
                        10
                );

        List<Map<String, Object>> rows =
                List.of(
                        Map.of(
                                "fieldA", "VALUE",
                                "unexpectedField", "BLOCK"
                        )
                );

        when(queryBuilder.build(plan, policy))
                .thenReturn(safeQuery);

        when(queryExecutor.execute(safeQuery))
                .thenReturn(rows);

        assertThrows(
                SecurityException.class,
                () -> executionService.execute(
                        plan,
                        policy
                )
        );
    }

    private DatabaseQueryPlan createPlan() {
        return new DatabaseQueryPlan(
                List.of("fieldA"),
                Map.of(),
                List.of(),
                List.of(),
                10
        );
    }

    private DatabaseSafeQueryPolicy createPolicy() {
        DatabaseValidationPolicy validationPolicy =
                new DatabaseValidationPolicy(
                        Set.of(
                                "rows",
                                "fieldA"
                        ),
                        Set.of(),
                        10,
                        10,
                        1000,
                        3,
                        true,
                        true
                );

        return new DatabaseSafeQueryPolicy(
                "SAFE_QUERY",
                "safe-query-test",
                "Safe Query Test",
                "mssql",
                "SAFE_TEXT_TO_SQL",
                "dbo.allowed_table",
                Map.of(
                        "fieldA", "column_a"
                ),
                Set.of(),
                10,
                Map.of(),
                validationPolicy
        );
    }

    @Test
    void executeAllowsEmptyResultAsNormalQueryResult() {
        DatabaseQueryPlan plan = createPlan();
        DatabaseSafeQueryPolicy policy = createPolicy();

        DatabaseSafeQuery safeQuery =
                new DatabaseSafeQuery(
                        "SELECT TOP (10) [column_a] AS [fieldA] "
                                + "FROM [dbo].[allowed_table]",
                        Map.of(),
                        10
                );

        when(queryBuilder.build(plan, policy))
                .thenReturn(safeQuery);

        when(queryExecutor.execute(safeQuery))
                .thenReturn(List.of());

        DatabaseQueryResult result =
                executionService.execute(
                        plan,
                        policy
                );

        assertEquals(
                List.of(),
                result.data().get("rows")
        );

        assertEquals(
                0,
                result.metadata().returnedCount()
        );

        assertNull(
                result.metadata().totalCount()
        );

        assertEquals(
                true,
                result.evidence().validated()
        );
    }
}