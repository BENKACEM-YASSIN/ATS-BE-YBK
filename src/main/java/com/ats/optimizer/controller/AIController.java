package com.ats.optimizer.controller;

import com.ats.optimizer.model.dto.ATSResult;
import com.ats.optimizer.model.dto.AnalyzeATSRequest;
import com.ats.optimizer.model.dto.EnhanceTextRequest;
import com.ats.optimizer.model.dto.ParseCVRequest;
import com.ats.optimizer.model.dto.TailoredBulletsRequest;
import com.ats.optimizer.service.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final GeminiService geminiService;

    @PostMapping("/enhance-text")
    public ResponseEntity<List<String>> enhanceText(@RequestBody EnhanceTextRequest request) {
        return response(geminiService.enhanceText(request.getText(), request.getType()));
    }

    @PostMapping("/generate-bullets")
    public ResponseEntity<List<String>> generateTailoredBullets(@RequestBody TailoredBulletsRequest request) {
        return response(geminiService.generateTailoredBullets(
                request.getDraftText(),
                request.getJobDescription(),
                request.getSectionType()
        ));
    }

    @PostMapping("/parse-cv")
    public ResponseEntity<JsonNode> parseCV(@RequestBody ParseCVRequest request) {
        return response(geminiService.parseCV(request.getText()));
    }

    @PostMapping("/analyze-ats")
    public ResponseEntity<ATSResult> analyzeATS(@RequestBody AnalyzeATSRequest request) {
        return response(geminiService.analyzeATS(request.getCvText(), request.getJobDescription()));
    }

    private <T> ResponseEntity<T> response(T body) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        String usedModel = geminiService.getUsedModel();
        if (usedModel != null && !usedModel.isBlank()) {
            response.header("X-Persona-AI-Model", usedModel);
        }
        return response.body(body);
    }
}
