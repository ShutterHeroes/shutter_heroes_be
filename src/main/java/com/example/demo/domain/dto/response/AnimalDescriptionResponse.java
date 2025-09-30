package com.example.demo.domain.dto.response;

import com.example.demo.domain.dto.openai.OpenAiAnimalInfoDto;
import com.example.demo.domain.entity.Species;
import com.example.demo.domain.enums.SpeciesStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimalDescriptionResponse {

    private UUID speciesId;
    private String scientificName;
    private String commonNameKo;
    private String commonNameEn;
    private SpeciesStatus status;
    private Boolean fromCache;
    private LocalDateTime timestamp;

    // OpenAI 상세 정보 (파싱된 객체)
    private OpenAiAnimalInfoDto detailInfo;

    /**
     * Species 엔티티로부터 응답 생성 (notes의 JSON을 파싱하여 detailInfo에 포함)
     * @param species Species 엔티티
     * @param fromCache DB 캐시에서 가져왔는지 여부
     * @param detailInfo 파싱된 OpenAI 상세 정보
     * @return AnimalDescriptionResponse
     */
    public static AnimalDescriptionResponse of(Species species, Boolean fromCache, OpenAiAnimalInfoDto detailInfo) {
        return AnimalDescriptionResponse.builder()
            .speciesId(species.getId())
            .scientificName(species.getScientificName())
            .commonNameKo(species.getCommonNameKo())
            .commonNameEn(species.getCommonNameEn())
            .status(species.getStatus())
            .fromCache(fromCache)
            .detailInfo(detailInfo)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
