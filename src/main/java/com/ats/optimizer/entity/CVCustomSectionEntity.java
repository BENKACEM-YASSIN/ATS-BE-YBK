package com.ats.optimizer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cv_custom_sections")
@Getter
@Setter
public class CVCustomSectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_document_id", nullable = false)
    @JsonIgnore
    private CVDocument cvDocument;

    private String sectionId;
    private Integer sortOrder;
    private String title;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String itemsJson;
}

