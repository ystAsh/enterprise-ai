/*
 * =============================================================================
 * 클래스명 : ChatApiController
 * =============================================================================
 * 목적
 *  - 로그인한 사용자의 자연어 질문을 HTTP 요청으로 전달받는다.
 *  - Spring Security가 인증한 CurrentUser를 서버에서 직접 가져온다.
 *  - AiChatService의 처리 결과를 공통 ChatResponse 형태로 반환한다.
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
}