/*
 * =============================================================================
 * 클래스명 : ChatApiController
 * =============================================================================
 * 목적
 *  - 로그인한 사용자의 자연어 질문을 HTTP 요청으로 전달받는다.
 *  - Spring Security가 인증한 CurrentUser를 서버에서 직접 가져온다.
 *  - 기존 동기 채팅 API와 진행 상태를 제공하는 Streaming 채팅 API를 제공한다.
 *  - AiChatService의 처리 결과를 공통 ChatResponse 형태로 반환한다.
 */

package com.example.enterpriseai.controller;

import com.example.enterpriseai.dto.ChatRequest;
import com.example.enterpriseai.dto.ChatResponse;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.chat.AiChatService;
import com.example.enterpriseai.service.chat.ChatProgressReporter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final AiChatService aiChatService;
    private final AsyncTaskExecutor taskExecutor;

    public ChatApiController(
            AiChatService aiChatService,
            @Qualifier("applicationTaskExecutor") AsyncTaskExecutor taskExecutor
    ) {
        this.aiChatService = aiChatService;
        this.taskExecutor = taskExecutor;
    }

    // 로그인 사용자의 질문과 서버가 인증한 CurrentUser를 AI 처리 계층에 전달한다.
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response =
                aiChatService.generateResponse(
                        request.question(),
                        currentUser
                );

        return ResponseEntity.ok(response);
    }

    // 채팅 처리 진행 상태와 최종 응답을 SSE로 전달한다.
    @PostMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter chatStream(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChatRequest request
    ) {
        SseEmitter emitter = new SseEmitter(0L);

        taskExecutor.execute(() ->
                processStream(
                        emitter,
                        request.question(),
                        currentUser
                )
        );

        return emitter;
    }

    // Progress Event와 최종 ChatResponse를 SSE Event로 전달한다.
    private void processStream(
            SseEmitter emitter,
            String question,
            CurrentUser currentUser
    ) {
        ChatProgressReporter progressReporter =
                event -> sendEvent(
                        emitter,
                        "progress",
                        event
                );

        try {
            ChatResponse response =
                    aiChatService.generateResponse(
                            question,
                            currentUser,
                            progressReporter
                    );

            sendEvent(
                    emitter,
                    "result",
                    response
            );

            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    // SSE 전송 실패 시 현재 채팅 처리를 중단할 수 있도록 예외를 전달한다.
    private void sendEvent(
            SseEmitter emitter,
            String eventName,
            Object data
    ) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(data)
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}