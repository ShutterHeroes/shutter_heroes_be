package com.example.demo.domain.dto.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String role;
    private String content;

    public static ChatMessage system(String content) {
        return ChatMessage.builder()
            .role("system")
            .content(content)
            .build();
    }

    public static ChatMessage user(String content) {
        return ChatMessage.builder()
            .role("user")
            .content(content)
            .build();
    }

    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
            .role("assistant")
            .content(content)
            .build();
    }
}
