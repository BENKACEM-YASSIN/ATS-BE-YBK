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
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AIController {

    private final GeminiService geminiService;

    @PostMapping("/enhance-text")
    public ResponseEntity<List<String>> enhanceText(@RequestBody EnhanceTextRequest request) {
        return ResponseEntity.ok(geminiService.enhanceText(request.getText(), request.getType()));
    }

    @PostMapping("/generate-bullets")
    public ResponseEntity<List<String>> generateTailoredBullets(@RequestBody TailoredBulletsRequest request) {
        return ResponseEntity.ok(geminiService.generateTailoredBullets(request.getDraftText(), request.getJobDescription()));
    }

    @PostMapping("/parse-cv")
    public ResponseEntity<JsonNode> parseCV(@RequestBody ParseCVRequest request) {
        return ResponseEntity.ok(geminiService.parseCV(request.getText()));
    }

    @PostMapping("/analyze-ats")
    public ResponseEntity<ATSResult> analyzeATS(@RequestBody AnalyzeATSRequest request) {
        return ResponseEntity.ok(geminiService.analyzeATS(request.getCvText(), request.getJobDescription()));
    }
}
