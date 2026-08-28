/*
 * =============================================================================
 * 클래스명 : DatabaseResultDownloadController
 * =============================================================================
 * 목적
 *  - resultReference로 검증 완료된 대량 Database 결과를 CSV로 다운로드한다.
 *  - Spring Security 현재 사용자를 기준으로 결과 소유권을 다시 검증한다.
 *  - 특정 업무 필드명을 하드코딩하지 않고 Map/List 구조를 동적으로 처리한다.
 */

package com.example.enterpriseai.controller;

import com.example.enterpriseai.dto.DatabaseResultReference;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.database.DatabaseResultReferenceStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/database/results")
public class DatabaseResultDownloadController {

    private final DatabaseResultReferenceStore resultReferenceStore;

    public DatabaseResultDownloadController(
            DatabaseResultReferenceStore resultReferenceStore
    ) {
        this.resultReferenceStore =
                resultReferenceStore;
    }

    @GetMapping(
            value = "/{resultReference}/download",
            produces = "text/csv"
    )
    public ResponseEntity<byte[]> download(
            @PathVariable String resultReference,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        DatabaseResultReference result =
                resultReferenceStore.getRequired(
                        resultReference,
                        currentUser
                );

        byte[] csv =
                createCsv(result.data());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"database-result.csv\""
                )
                .contentType(
                        new MediaType(
                                "text",
                                "csv",
                                StandardCharsets.UTF_8
                        )
                )
                .body(csv);
    }

    /*
     * 공통 AI 계층에서 items 같은 특정 키를 가정하지 않는다.
     *
     * 검증 완료 data 내부에서 CSV로 표현 가능한
     * List<Map<String, Object>> 구조를 하나만 허용한다.
     */
    private byte[] createCsv(
            Map<String, Object> data
    ) {

        List<Map<String, Object>> rows =
                findRows(data);

        Set<String> columns =
                new LinkedHashSet<>();

        for (Map<String, Object> row : rows) {
            columns.addAll(
                    row.keySet()
            );
        }

        StringBuilder csv =
                new StringBuilder();

        csv.append('\uFEFF');

        appendRow(
                csv,
                new ArrayList<>(columns)
        );

        for (Map<String, Object> row : rows) {

            List<String> values =
                    new ArrayList<>();

            for (String column : columns) {

                Object value =
                        row.get(column);

                values.add(
                        value == null
                                ? ""
                                : String.valueOf(value)
                );
            }

            appendRow(
                    csv,
                    values
            );
        }

        return csv.toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    private List<Map<String, Object>> findRows(
            Map<String, Object> data
    ) {

        List<List<Map<String, Object>>> candidates =
                new ArrayList<>();

        for (Object value : data.values()) {

            if (!(value instanceof List<?> list)) {
                continue;
            }

            List<Map<String, Object>> rows =
                    convertRows(list);

            if (rows != null) {
                candidates.add(rows);
            }
        }

        if (candidates.size() != 1) {
            throw new IllegalStateException(
                    "다운로드 가능한 결과 구조가 아닙니다."
            );
        }

        return candidates.getFirst();
    }

    private List<Map<String, Object>> convertRows(
            List<?> list
    ) {

        if (list.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> rows =
                new ArrayList<>();

        for (Object item : list) {

            if (!(item instanceof Map<?, ?> map)) {
                return null;
            }

            Map<String, Object> row =
                    new java.util.LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {

                if (!(entry.getKey() instanceof String key)) {
                    return null;
                }

                row.put(
                        key,
                        entry.getValue()
                );
            }

            rows.add(row);
        }

        return rows;
    }

    private void appendRow(
            StringBuilder csv,
            List<String> values
    ) {

        for (int i = 0; i < values.size(); i++) {

            if (i > 0) {
                csv.append(',');
            }

            csv.append(
                    escapeCsv(values.get(i))
            );
        }

        csv.append("\r\n");
    }

    private String escapeCsv(
            String value
    ) {

        String safeValue =
                value == null
                        ? ""
                        : value;

        /*
         * Spreadsheet Formula Injection 방지.
         */
        if (!safeValue.isEmpty()) {

            char first =
                    safeValue.charAt(0);

            if (first == '='
                    || first == '+'
                    || first == '-'
                    || first == '@') {

                safeValue =
                        "'" + safeValue;
            }
        }

        return "\""
                + safeValue.replace(
                "\"",
                "\"\""
        )
                + "\"";
    }
}