package com.ats.optimizer.model.dto;

import lombok.Data;

@Data
public class TailoredBulletsRequest {
    private String draftText;
    private String jobDescription;
    private String sectionType; // job | summary | education | custom | skill
}
