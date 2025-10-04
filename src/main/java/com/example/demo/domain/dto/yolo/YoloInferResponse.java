package com.example.demo.domain.dto.yolo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoloInferResponse {

    private String requestId;
    private String status;
}
