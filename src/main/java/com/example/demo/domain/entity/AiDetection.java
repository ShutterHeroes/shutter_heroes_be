package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_detections", schema = "app")
@Getter
@Setter
@NoArgsConstructor
public class AiDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;

    @Column
    private BigDecimal score;

    @Column(name = "x_min")
    private BigDecimal xMin;

    @Column(name = "y_min")
    private BigDecimal yMin;

    @Column(name = "x_max")
    private BigDecimal xMax;

    @Column(name = "y_max")
    private BigDecimal yMax;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
