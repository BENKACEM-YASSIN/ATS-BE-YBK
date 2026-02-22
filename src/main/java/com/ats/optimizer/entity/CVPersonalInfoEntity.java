package com.ats.optimizer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cv_personal_info")
@Getter
@Setter
public class CVPersonalInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_document_id", nullable = false, unique = true)
    @JsonIgnore
    private CVDocument cvDocument;

    private String firstName;
    private String lastName;
    private String title;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private String website;
    private String linkedin;

    @Column(columnDefinition = "TEXT")
    private String aboutMe;
}

