package com.example.demo.domain.dto.vision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBox {

    private Integer x1;
    private Integer y1;
    private Integer x2;
    private Integer y2;
    private Integer width;
    private Integer height;

    public static BoundingBox of(Integer x1, Integer y1, Integer x2, Integer y2) {
        return BoundingBox.builder()
            .x1(x1)
            .y1(y1)
            .x2(x2)
            .y2(y2)
            .width(x2 - x1)
            .height(y2 - y1)
            .build();
    }
}
