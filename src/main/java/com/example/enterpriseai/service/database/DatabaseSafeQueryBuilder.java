/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQueryBuilder
 * =============================================================================
 * 목적
 *  - 검증 완료 DatabaseQueryPlan과 서버 내부 DatabaseSafeQueryPolicy를 이용해
 *    실행 가능한 MSSQL SELECT Query를 생성한다.
 *  - LLM이 생성한 SQL을 사용하지 않고 Java 서버가 SQL 구조를 직접 구성한다.
 *  - 조회 대상과 실제 Column은 서버 정책의 매핑만 사용한다.
 *  - Filter 값은 SQL 문자열에 직접 결합하지 않고 Binding Parameter로 분리한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryPlan;
import com.example.enterpriseai.dto.DatabaseSafeQuery;
import com.example.enterpriseai.dto.DatabaseSafeQueryPolicy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DatabaseSafeQueryBuilder {

    private static final Pattern SQL_IDENTIFIER =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    // 검증 완료 Plan과 서버 정책만 사용하여 MSSQL SELECT Query를 생성한다.
    public DatabaseSafeQuery build(
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

        if (!plan.groupByFields().isEmpty()) {
            throw new IllegalArgumentException(
                    "현재 Safe Query에서는 GROUP BY를 지원하지 않습니다."
            );
        }

        int maxRows = Math.min(
                plan.maxRows(),
                policy.maxRows()
        );

        String selectClause = buildSelectClause(
                plan.selectFields(),
                policy
        );

        String fromClause = quoteQualifiedIdentifier(
                policy.tableName()
        );

        Map<String, Object> parameters = new LinkedHashMap<>();

        String whereClause = buildWhereClause(
                plan.filters(),
                policy,
                parameters
        );

        String orderByClause = buildOrderByClause(
                plan.orderByFields(),
                policy
        );

        StringBuilder sql = new StringBuilder()
                .append("SELECT TOP (")
                .append(maxRows)
                .append(") ")
                .append(selectClause)
                .append(" FROM ")
                .append(fromClause);

        if (!whereClause.isBlank()) {
            sql.append(" WHERE ")
                    .append(whereClause);
        }

        if (!orderByClause.isBlank()) {
            sql.append(" ORDER BY ")
                    .append(orderByClause);
        }

        return new DatabaseSafeQuery(
                sql.toString(),
                parameters,
                maxRows
        );
    }

    private String buildSelectClause(
            List<String> selectFields,
            DatabaseSafeQueryPolicy policy
    ) {
        return selectFields.stream()
                .map(field -> {
                    String column = getRequiredColumn(
                            field,
                            policy
                    );

                    return quoteQualifiedIdentifier(column)
                            + " AS "
                            + quoteIdentifier(field);
                })
                .collect(Collectors.joining(", "));
    }

    private String buildWhereClause(
            Map<String, Object> filters,
            DatabaseSafeQueryPolicy policy,
            Map<String, Object> parameters
    ) {
        if (filters.isEmpty()) {
            return "";
        }

        int index = 0;
        StringBuilder where = new StringBuilder();

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (index > 0) {
                where.append(" AND ");
            }

            String column = getRequiredColumn(
                    entry.getKey(),
                    policy
            );

            String parameterName = "p" + index;

            where.append(quoteQualifiedIdentifier(column))
                    .append(" = :")
                    .append(parameterName);

            parameters.put(
                    parameterName,
                    entry.getValue()
            );

            index++;
        }

        return where.toString();
    }

    private String buildOrderByClause(
            List<String> orderByFields,
            DatabaseSafeQueryPolicy policy
    ) {
        if (orderByFields.isEmpty()) {
            return "";
        }

        return orderByFields.stream()
                .map(field -> {
                    if (!policy.allowedOrderByFields().contains(field)) {
                        throw new IllegalArgumentException(
                                "허용되지 않은 정렬 필드입니다: " + field
                        );
                    }

                    return quoteQualifiedIdentifier(
                            getRequiredColumn(field, policy)
                    ) + " ASC";
                })
                .collect(Collectors.joining(", "));
    }

    private String getRequiredColumn(
            String logicalField,
            DatabaseSafeQueryPolicy policy
    ) {
        String column = policy.fieldMappings().get(logicalField);

        if (column == null || column.isBlank()) {
            throw new IllegalArgumentException(
                    "서버에 등록되지 않은 필드입니다: "
                            + logicalField
            );
        }

        return column.trim();
    }

    private String quoteQualifiedIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL 식별자 정보가 없습니다."
            );
        }

        return java.util.Arrays.stream(identifier.trim().split("\\."))
                .map(this::quoteIdentifier)
                .collect(Collectors.joining("."));
    }

    private String quoteIdentifier(String identifier) {
        if (identifier == null
                || !SQL_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "허용되지 않은 SQL 식별자입니다."
            );
        }

        return "[" + identifier + "]";
    }
}