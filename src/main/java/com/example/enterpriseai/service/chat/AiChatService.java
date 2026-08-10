/*
 * =============================================================================
 * 클래스명 : AiChatService
 * =============================================================================
 * 목적
 *  - 사용자의 질문을 Gemini에 전달하고 답변을 생성한다.
 *  - Controller와 Spring AI ChatClient 사이의 서비스 계층을 담당한다.
 *  - 현재 단계에서는 기본 Gemini 호출만 처리한다.
 */

package com.example.enterpriseai.service.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final ChatClient chatClient;

    public AiChatService(
            ChatClient.Builder chatClientBuilder
    ) {
        // Spring Boot가 자동 생성한 Builder로 프로젝트용 ChatClient를 생성한다.
        this.chatClient = chatClientBuilder.build();
    }

    // 사용자 질문을 Gemini에 전달하고 생성된 답변 문자열을 반환한다.
    public String generateAnswer(
            String question
    ) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }
}