/*
 * =============================================================================
 * 클래스명 : DatabaseResultPresentationPolicy
 * =============================================================================
 * 목적
 *  - 검증 완료된 Database Query 결과의 사용자 제공 방식을 판단한다.
 *  - 소량 결과와 대량 결과의 기준을 공통 정책으로 관리한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import org.springframework.stereotype.Component;

@Component
public class DatabaseResultPresentationPolicy {

    private static final int INLINE_MAX_ROWS = 20;

    public PresentationType determine(int rowCount) {

        if (rowCount < 0) {
            throw new IllegalArgumentException(
                    "조회 결과 건수는 0 이상이어야 합니다."
            );
        }

        if (rowCount <= INLINE_MAX_ROWS) {
            return PresentationType.INLINE;
        }

        return PresentationType.EXTERNAL;
    }

    public int inlineMaxRows() {
        return INLINE_MAX_ROWS;
    }

    public enum PresentationType {
        INLINE,
        EXTERNAL
    }
}