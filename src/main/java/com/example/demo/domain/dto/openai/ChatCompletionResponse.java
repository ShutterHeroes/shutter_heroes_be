package com.example.demo.domain.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Integer index;
        private ChatMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            ChatMessage message = choices.get(0).getMessage();
            Object content = message.getContent();

            // String인 경우 그대로 반환
            if (content instanceof String) {
                return (String) content;
            }

            // getContentAsString() 메서드 사용
            return message.getContentAsString();
        }
        return null;
    }
}
