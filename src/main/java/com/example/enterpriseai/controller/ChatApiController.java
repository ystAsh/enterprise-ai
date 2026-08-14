/*
 * =============================================================================
 * 클래스명 : ChatApiController
 * =============================================================================
 * 목적
 *  - 로그인한 사용자의 자연어 질문을 HTTP 요청으로 전달받는다.
 *  - Spring Security가 인증한 CurrentUser를 서버에서 직접 가져온다.
 *  - AiChatService를 호출하여 Gemini 답변을 생성한다.
 */

package com.example.enterpriseai.controller;

import com.example.enterpriseai.dto.ChatRequest;
import com.example.enterpriseai.dto.ChatResponse;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.chat.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// JSON 기반 채팅 API를 제공하는 Controller로 등록한다.
@RestController

// 이 Controller의 기본 URL을 /api/chat으로 지정한다.
@RequestMapping("/api/chat")
public class ChatApiController {

    private final AiChatService aiChatService;

    public ChatApiController(
            AiChatService aiChatService
    ) {
        this.aiChatService = aiChatService;
    }

    // 로그인 사용자의 질문을 받고 Spring Security Principal을 함께 가져온다.
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChatRequest request
    ) {

        // 현재는 Gemini 기본 호출만 수행하며 CurrentUser 연결 여부만 확인한다.
        String answer =
                aiChatService.generateAnswer(
                        request.question()
                );

        return ResponseEntity.ok(
                new ChatResponse(answer)
        );
    }
}