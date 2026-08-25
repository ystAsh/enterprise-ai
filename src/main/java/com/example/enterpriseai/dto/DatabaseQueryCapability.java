/*
 * =============================================================================
 * 클래스명 : DatabaseQueryCapability
 * =============================================================================
 * 목적
 *  - 자연어 질문과 서버에 허용된 조회 기능을 연결하기 위한
 *    최소한의 안전한 Capability 정보를 표현한다.
 *  - 실제 SQL, 테이블, 컬럼, Repository, Mapper, 권한 정책 등의
 *    서버 내부 실행 정보를 포함하지 않는다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.dto;

import java.util.Set;

public record DatabaseQueryCapability(

        // Query Definition의 queryKey와 분리된
        // 검색/선택용 안전한 식별자
        String capabilityKey,

        // 사용 가능한 조회 기능의 자연어 설명
        String description,

        // 이 Capability가 지원하는 논리적 동작
        // 예: SEARCH, LIST, SUMMARY, COMPARE
        // 특정 업무명이나 DB 구조를 넣지 않는다.
        Set<String> supportedIntents

) {

    public DatabaseQueryCapability {

        if (capabilityKey == null || capabilityKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Capability 식별자가 없습니다."
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Capability 설명이 없습니다."
            );
        }

        supportedIntents =
                supportedIntents == null
                        ? Set.of()
                        : Set.copyOf(supportedIntents);

        if (supportedIntents.isEmpty()) {
            throw new IllegalArgumentException(
                    "Capability 지원 동작이 없습니다."
            );
        }
    }
}