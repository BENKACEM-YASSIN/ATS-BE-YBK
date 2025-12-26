package com.ats.optimizer.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProfileDTO {
    private String firstName;
    private String lastName;
    private String title;
    private String email;
    private String motto;
    private String summary;
    private String photoUrl;
    private String drivingLicence;
    private List<String> hobbies;
    private List<String> interests;
    private Integer profileProgress;
    private LocalDateTime lastVisit;
    private String lastEditedFileName;
    private LocalDateTime lastEditedDate;
    
    // Stats
    private long cvCount;
    private long coverLetterCount;
    private int yearsOfExperience;
    private int workExperienceCount;
    private int educationCount;
    private int skillsCount;
    private int languagesCount;
}
