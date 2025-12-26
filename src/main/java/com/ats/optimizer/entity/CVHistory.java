package com.ats.optimizer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "cv_history")
@Data
public class CVHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String cvDataJson;

    private boolean pinned = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
