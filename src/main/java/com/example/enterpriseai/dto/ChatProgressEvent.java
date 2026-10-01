/*
 * =============================================================================
 * 클래스명 : ChatProgressEvent
 * =============================================================================
 * 목적
 *  - 채팅 처리 중 사용자에게 노출 가능한 진행 상태를 전달한다.
 *  - SQL, queryKey, Parameter, 내부 API 등 내부 실행 정보를 포함하지 않는다.
 */

package com.example.enterpriseai.dto;

public record ChatProgressEvent(
        Stage stage,
        String displayText,
        String iconKey
) {

    public ChatProgressEvent {
        if (stage == null) {
            throw new IllegalArgumentException("stage must not be null");
        }

        if (displayText == null || displayText.isBlank()) {
            throw new IllegalArgumentException("displayText must not be blank");
        }

        if (iconKey == null || iconKey.isBlank()) {
            throw new IllegalArgumentException("iconKey must not be blank");
        }
    }

    public enum Stage {
        QUESTION_ANALYSIS,
        DOCUMENT_SEARCH,
        DATABASE_SEARCH,
        ANSWER_GENERATION,
        ANSWER_VERIFICATION
    }
}