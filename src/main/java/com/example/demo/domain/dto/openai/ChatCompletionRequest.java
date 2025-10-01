package com.example.demo.domain.dto.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {

    private String model;
    private List<ChatMessage> messages;

    public static ChatCompletionRequest of(String model, List<ChatMessage> messages, Double temperature, Integer maxTokens) {
        return ChatCompletionRequest.builder()
            .model(model)
            .messages(messages)
            .build();
    }
}
