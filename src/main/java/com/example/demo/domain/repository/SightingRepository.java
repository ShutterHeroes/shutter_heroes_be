package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Sighting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Sighting 엔티티의 데이터베이스 접근을 담당하는 Repository
 */
@Repository
public interface SightingRepository extends JpaRepository<Sighting, UUID> {
}
