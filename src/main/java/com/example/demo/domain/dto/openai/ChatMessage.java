package com.example.demo.domain.dto.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completion API의 메시지 DTO
 *
 * <p>텍스트 전용 또는 Vision API (텍스트 + 이미지) 모두 지원</p>
 *
 * <p><b>텍스트 전용 예시:</b></p>
 * <pre>{@code
 * ChatMessage.user("고양이에 대해 알려주세요")
 * }</pre>
 *
 * <p><b>Vision API (이미지 포함) 예시:</b></p>
 * <pre>{@code
 * ChatMessage.userWithImage("이 동물의 학명을 알려주세요", base64EncodedImage)
 * }</pre>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String role;

    /**
     * 메시지 내용
     * - 텍스트 전용: String
     * - Vision API: List<Map<String, Object>> (text + image_url)
     */
    private Object content;

    /**
     * 시스템 메시지 생성 (텍스트 전용)
     */
    public static ChatMessage system(String content) {
        return ChatMessage.builder()
            .role("system")
            .content(content)
            .build();
    }

    /**
     * 사용자 메시지 생성 (텍스트 전용)
     */
    public static ChatMessage user(String content) {
        return ChatMessage.builder()
            .role("user")
            .content(content)
            .build();
    }

    /**
     * 어시스턴트 메시지 생성 (텍스트 전용)
     */
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
            .role("assistant")
            .content(content)
            .build();
    }

    /**
     * 사용자 메시지 생성 (Vision API: 텍스트 + 이미지)
     *
     * @param text 질문 텍스트
     * @param base64Image Base64로 인코딩된 이미지 (data:image/jpeg;base64,... 형식)
     * @return Vision API용 ChatMessage
     */
    public static ChatMessage userWithImage(String text, String base64Image) {
        List<Map<String, Object>> contentParts = new ArrayList<>();

        // 1. 텍스트 파트
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", text);
        contentParts.add(textPart);

        // 2. 이미지 파트
        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");

        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", base64Image);
        imagePart.put("image_url", imageUrl);

        contentParts.add(imagePart);

        return ChatMessage.builder()
            .role("user")
            .content(contentParts)
            .build();
    }

    /**
     * content를 String으로 반환 (텍스트 전용 메시지인 경우)
     * Vision API 응답 파싱 시 사용
     */
    public String getContentAsString() {
        if (content instanceof String) {
            return (String) content;
        }
        return null;
    }
}
