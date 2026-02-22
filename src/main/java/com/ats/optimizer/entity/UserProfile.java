package com.ats.optimizer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
@Data
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String title;
    private String email;
    private String phone;
    
    private String motto;
    
    @Column(length = 2000)
    private String summary;
    
    @Column(columnDefinition = "TEXT")
    private String photoUrl; // Can store Base64
    
    private String drivingLicence;
    
    @ElementCollection
    private List<String> hobbies = new ArrayList<>();
    
    @ElementCollection
    private List<String> interests = new ArrayList<>();
    
    private Integer profileProgress = 0;
    
    private LocalDateTime lastVisit;
    private LocalDateTime lastEdited;
    private String lastEditedFileName;
    
    @Column(columnDefinition = "TEXT")
    private String cvDataJson; 

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cv_document_id")
    @JsonIgnore
    private CVDocument cvDocument;
}
