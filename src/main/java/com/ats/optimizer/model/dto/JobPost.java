package com.ats.optimizer.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobPost {
    private String id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String url;
    private String email; // Extracted email from job post
    private String postedDate;
    private Double matchScore; // ATS match score (0-100)
    private String matchReasoning;
    private List<String> requiredSkills;
    private String jobType; // Full-time, Part-time, Contract, etc.
    private String salary;
}
