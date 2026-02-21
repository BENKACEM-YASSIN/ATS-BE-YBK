package com.ats.optimizer.model.dto;

import lombok.Data;

@Data
public class JobSearchRequest {
    private String location; // Location from profile
    private String jobTitle; // Title from profile
    private Integer maxResults = 24; // Default to 24 as requested
    private Integer daysBack = 1; // Search last 24 hours
}
