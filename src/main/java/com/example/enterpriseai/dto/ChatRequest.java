/*
 * =============================================================================
 * 클래스명 : ChatRequest
 * =============================================================================
 * 목적
 *  - 사용자가 입력한 자연어 질문을 전달받는다.
 *  - 빈 질문과 지나치게 긴 질문이 서비스로 전달되지 않도록 검증한다.
 */

package com.example.enterpriseai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(

        // 빈 문자열과 공백만 입력된 질문을 차단한다.
        @NotBlank(
                message = "질문을 입력해 주세요."
        )

        // 과도하게 긴 입력으로 인한 토큰 사용과 처리 비용을 제한한다.
        @Size(
                max = 2000,
                message = "질문은 2,000자를 초과할 수 없습니다."
        )
        String question
) {
}