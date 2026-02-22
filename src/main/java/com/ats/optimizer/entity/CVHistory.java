package com.ats.optimizer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
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

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cv_document_id")
    @JsonIgnore
    private CVDocument cvDocument;

    @Transient
    private JsonNode cvData;

    private boolean pinned = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
