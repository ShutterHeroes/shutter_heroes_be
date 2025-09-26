package com.example.demo.domain.entity;

import com.example.demo.domain.enums.SpeciesStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "species", schema = "app")
@Getter
@Setter
@NoArgsConstructor
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "common_name_ko", nullable = false)
    private String commonNameKo;

    @Column(name = "common_name_en")
    private String commonNameEn;

    @Column(name = "scientific_name")
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpeciesStatus status = SpeciesStatus.COMMON;

    @Column(name = "protected_code")
    private String protectedCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
