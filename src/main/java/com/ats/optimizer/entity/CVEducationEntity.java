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
@Table(name = "cv_educations")
@Getter
@Setter
public class CVEducationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_document_id", nullable = false)
    @JsonIgnore
    private CVDocument cvDocument;

    private String itemId;
    private Integer sortOrder;
    private String degree;
    private String school;
    private String city;
    private String country;
    private String startDate;
    private String endDate;

    @Column(name = "is_current")
    private Boolean currentEducation;

    @Column(columnDefinition = "TEXT")
    private String description;
}

