package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Species;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpeciesRepository extends JpaRepository<Species, UUID> {

    /**
     * 학명으로 종 정보 조회
     * @param scientificName 학명 (예: "Panthera leo")
     * @return Optional<Species>
     */
    Optional<Species> findByScientificName(String scientificName);

    /**
     * 학명 존재 여부 확인
     * @param scientificName 학명
     * @return 존재 여부
     */
    boolean existsByScientificName(String scientificName);
}