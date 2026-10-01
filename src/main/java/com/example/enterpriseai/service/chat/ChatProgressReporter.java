/*
 * =============================================================================
 * 클래스명 : ChatProgressReporter
 * =============================================================================
 * 목적
 *  - 채팅 처리 과정에서 발생한 사용자 표시용 진행 상태를 외부 전달 계층에 보고한다.
 *  - 서비스 계층이 SSE, SseEmitter 등 특정 전송 기술에 직접 의존하지 않도록 한다.
 *  - 기존 동기 채팅 경로에서는 NO_OP Reporter를 사용하여 기존 API 동작을 유지할 수 있다.
 */

package com.example.enterpriseai.service.chat;

import com.example.enterpriseai.dto.ChatProgressEvent;

@FunctionalInterface
public interface ChatProgressReporter {

    ChatProgressReporter NO_OP = event -> {
        // 기존 동기 채팅 경로에서는 진행 상태를 전달하지 않는다.
    };

    void report(ChatProgressEvent event);
}