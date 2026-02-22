package com.ats.optimizer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cv_documents")
@Getter
@Setter
public class CVDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String theme;

    @Column(columnDefinition = "TEXT")
    private String sectionOrderJson;

    @Column(columnDefinition = "TEXT")
    private String sectionTitlesJson;

    @Column(columnDefinition = "TEXT")
    private String sectionColumnsJson;

    @OneToOne(mappedBy = "cvDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private CVPersonalInfoEntity personalInfo;

    @OneToMany(mappedBy = "cvDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CVWorkExperienceEntity> workExperiences = new ArrayList<>();

    @OneToMany(mappedBy = "cvDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CVEducationEntity> educations = new ArrayList<>();

    @OneToOne(mappedBy = "cvDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private CVSkillsEntity skills;

    @OneToMany(mappedBy = "cvDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CVCustomSectionEntity> customSections = new ArrayList<>();

    public void setPersonalInfo(CVPersonalInfoEntity personalInfo) {
        this.personalInfo = personalInfo;
        if (personalInfo != null) {
            personalInfo.setCvDocument(this);
        }
    }

    public void setSkills(CVSkillsEntity skills) {
        this.skills = skills;
        if (skills != null) {
            skills.setCvDocument(this);
        }
    }

    public void setWorkExperiences(List<CVWorkExperienceEntity> workExperiences) {
        this.workExperiences.clear();
        if (workExperiences == null) return;
        for (CVWorkExperienceEntity item : workExperiences) {
            addWorkExperience(item);
        }
    }

    public void setEducations(List<CVEducationEntity> educations) {
        this.educations.clear();
        if (educations == null) return;
        for (CVEducationEntity item : educations) {
            addEducation(item);
        }
    }

    public void setCustomSections(List<CVCustomSectionEntity> customSections) {
        this.customSections.clear();
        if (customSections == null) return;
        for (CVCustomSectionEntity item : customSections) {
            addCustomSection(item);
        }
    }

    public void addWorkExperience(CVWorkExperienceEntity item) {
        if (item == null) return;
        item.setCvDocument(this);
        this.workExperiences.add(item);
    }

    public void addEducation(CVEducationEntity item) {
        if (item == null) return;
        item.setCvDocument(this);
        this.educations.add(item);
    }

    public void addCustomSection(CVCustomSectionEntity item) {
        if (item == null) return;
        item.setCvDocument(this);
        this.customSections.add(item);
    }
}

