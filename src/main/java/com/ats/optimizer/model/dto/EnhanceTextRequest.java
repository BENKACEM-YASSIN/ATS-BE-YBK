package com.ats.optimizer.model.dto;

import lombok.Data;

@Data
public class EnhanceTextRequest {
    private String text;
    private String type; // job | summary | education | custom | skill
}
