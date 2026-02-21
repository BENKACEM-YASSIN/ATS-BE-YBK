package com.ats.optimizer.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobSearchResponse {
    private List<JobPost> jobs;
    private Integer totalFound;
    private String searchLocation;
    private String searchTitle;
}
