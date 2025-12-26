package com.ats.optimizer.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class ATSResult {
    private int score;
    private String matchReasoning;
    private List<String> missingKeywords;
    private List<String> suggestions;
}
