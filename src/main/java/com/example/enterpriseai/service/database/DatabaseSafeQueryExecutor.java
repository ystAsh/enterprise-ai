/*
 * =============================================================================
 * 클래스명 : DatabaseSafeQueryExecutor
 * =============================================================================
 * 목적
 *  - Java 서버가 생성한 DatabaseSafeQuery만 MSSQL에서 실행한다.
 *  - Safe Text-to-SQL 실행 시 Named Parameter Binding을 강제한다.
 *  - 조회 결과가 서버에서 허용한 최대 건수를 초과하지 않는지 다시 확인한다.
 *  - LLM이 직접 생성한 SQL이나 외부 입력 SQL을 실행하지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseSafeQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseSafeQueryExecutor {

    private final JdbcClient jdbcClient;

    public DatabaseSafeQueryExecutor(
            @Qualifier("safeQueryJdbcClient")
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    // Java Safe Query Builder가 생성한 SELECT를 Named Parameter Binding으로 실행한다.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> execute(
            DatabaseSafeQuery safeQuery
    ) {
        if (safeQuery == null) {
            throw new IllegalArgumentException(
                    "실행할 Safe Query가 없습니다."
            );
        }

        List<Map<String, Object>> rows = jdbcClient
                .sql(safeQuery.sql())
                .params(safeQuery.parameters())
                .query(new ColumnMapRowMapper())
                .list();

        if (rows.size() > safeQuery.maxRows()) {
            throw new IllegalStateException(
                    "Safe Query 최대 조회 건수를 초과했습니다."
            );
        }

        return List.copyOf(rows);
    }
}