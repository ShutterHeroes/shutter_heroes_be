package com.example.demo.domain.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * OpenAI API로부터 반환받는 동물 정보 DTO
 * JSON 형식으로 파싱됨
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiAnimalInfoDto {

    @JsonProperty("한국명")
    private String commonNameKo;

    @JsonProperty("영문명")
    private String commonNameEn;

    @JsonProperty("분류학적 정보")
    private Map<String, String> taxonomicInfo;

    @JsonProperty("신체적 특징")
    private String physicalCharacteristics;

    @JsonProperty("서식지 및 분포 지역")
    private String habitat;

    @JsonProperty("생활 습성")
    private String behavior;

    @JsonProperty("보존 상태")
    private String conservationStatus;

    @JsonProperty("생태계에서의 역할")
    private String ecosystemRole;

    @JsonProperty("인간과의 관계")
    private String humanRelation;

    @JsonProperty("흥미로운 사실")
    private String interestingFacts;
}
