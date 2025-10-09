package com.example.demo.domain.dto.yolo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoloInferRequest {

    private List<String> urls;
    @JsonProperty("callback_url")
    private String callbackUrl;

    public static YoloInferRequest of(List<String> urls, String callbackUrl) {
        return YoloInferRequest.builder()
            .urls(urls)
            .callbackUrl(callbackUrl)
            .build();
    }
}
