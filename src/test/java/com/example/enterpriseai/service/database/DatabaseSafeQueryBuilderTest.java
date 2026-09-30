/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQueryBuilderTest
 * =============================================================================
 * 목적
 *  - DatabaseSafeQueryBuilder가 서버 정책 범위 안에서만 MSSQL SELECT를 생성하는지 검증한다.
 *  - Filter 값이 SQL 문자열에 직접 결합되지 않고 Binding Parameter로 분리되는지 확인한다.
 *  - 미등록 필드와 지원하지 않는 GROUP BY 요청이 차단되는지 확인한다.
 *  - 서버의 maxRows 제한이 최종 SQL에 강제되는지 확인한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryPlan;
import com.example.enterpriseai.dto.DatabaseSafeQuery;
import com.example.enterpriseai.dto.DatabaseSafeQueryPolicy;
import com.example.enterpriseai.dto.DatabaseValidationPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSafeQueryBuilderTest {

    private final DatabaseSafeQueryBuilder builder =
            new DatabaseSafeQueryBuilder();

    @Test
    void buildCreatesMssqlSelectWithBindingParameters() {
        DatabaseQueryPlan plan = new DatabaseQueryPlan(
                List.of("fieldA", "fieldB"),
                Map.of("filterField", "TEST"),
                List.of(),
                List.of("fieldA"),
                20
        );

        DatabaseSafeQueryPolicy policy =
                createPolicy(100);

        DatabaseSafeQuery query =
                builder.build(plan, policy);

        assertEquals(
                "SELECT TOP (20) "
                        + "[column_a] AS [fieldA], "
                        + "[column_b] AS [fieldB] "
                        + "FROM [dbo].[allowed_table] "
                        + "WHERE [filter_column] = :p0 "
                        + "ORDER BY [column_a] ASC",
                query.sql()
        );

        assertEquals(
                Map.of("p0", "TEST"),
                query.parameters()
        );

        assertEquals(
                20,
                query.maxRows()
        );
    }

    @Test
    void buildDoesNotConcatenateFilterValueIntoSql() {
        String filterValue = "' OR 1=1 --";

        DatabaseQueryPlan plan = new DatabaseQueryPlan(
                List.of("fieldA"),
                Map.of("filterField", filterValue),
                List.of(),
                List.of(),
                10
        );

        DatabaseSafeQuery query =
                builder.build(
                        plan,
                        createPolicy(100)
                );

        assertFalse(
                query.sql().contains(filterValue)
        );

        assertTrue(
                query.sql().contains(
                        "[filter_column] = :p0"
                )
        );

        assertEquals(
                filterValue,
                query.parameters().get("p0")
        );
    }

    @Test
    void buildRejectsUnregisteredField() {
        DatabaseQueryPlan plan = new DatabaseQueryPlan(
                List.of("unknownField"),
                Map.of(),
                List.of(),
                List.of(),
                10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.build(
                        plan,
                        createPolicy(100)
                )
        );
    }

    @Test
    void buildRejectsGroupByUntilSupported() {
        DatabaseQueryPlan plan = new DatabaseQueryPlan(
                List.of("fieldA"),
                Map.of(),
                List.of("fieldA"),
                List.of(),
                10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.build(
                        plan,
                        createPolicy(100)
                )
        );
    }

    @Test
    void buildUsesSmallerServerMaxRows() {
        DatabaseQueryPlan plan = new DatabaseQueryPlan(
                List.of("fieldA"),
                Map.of(),
                List.of(),
                List.of(),
                100
        );

        DatabaseSafeQuery query =
                builder.build(
                        plan,
                        createPolicy(30)
                );

        assertEquals(
                30,
                query.maxRows()
        );

        assertTrue(
                query.sql().startsWith(
                        "SELECT TOP (30)"
                )
        );
    }

    private DatabaseSafeQueryPolicy createPolicy(
            int maxRows
    ) {
        DatabaseValidationPolicy validationPolicy =
                new DatabaseValidationPolicy(
                        Set.of(
                                "rows",
                                "fieldA",
                                "fieldB",
                                "filterField"
                        ),
                        Set.of(),
                        10,
                        maxRows,
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
                        "fieldA", "column_a",
                        "fieldB", "column_b",
                        "filterField", "filter_column"
                ),
                Set.of("fieldA"),
                maxRows,
                Map.of(),
                validationPolicy
        );
    }
}