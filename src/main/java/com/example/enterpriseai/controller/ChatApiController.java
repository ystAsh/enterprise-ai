/*
 * =============================================================================
 * 클래스명 : ChatApiController
 * =============================================================================
 * 목적
 *  - 사용자의 자연어 질문을 HTTP 요청으로 전달받는다.
 *  - AiChatService를 호출하여 Gemini 답변을 생성한다.
 *  - 생성된 답변을 ChatResponse 형식으로 반환한다.
 */

package com.example.enterpriseai.controller;

import com.example.enterpriseai.dto.ChatRequest;
import com.example.enterpriseai.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.enterpriseai.service.chat.AiChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final AiChatService aiChatService;

    // Spring이 관리하는 AiChatService를 생성자로 주입받는다.
    public ChatApiController(
            AiChatService aiChatService
    ) {
        this.aiChatService =
                aiChatService;
    }

    // 사용자 질문을 검증한 후 Gemini 답변을 반환한다.
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request
    ) {
        String answer =
                aiChatService.generateAnswer(
                        request.question()
                );

        return ResponseEntity.ok(
                new ChatResponse(answer)
        );
    }
}