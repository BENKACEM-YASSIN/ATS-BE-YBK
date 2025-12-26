package com.ats.optimizer.controller;

import com.ats.optimizer.service.PdfService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    @PostMapping("/extract-text")
    public ResponseEntity<Map<String, String>> extractText(@RequestParam("file") MultipartFile file) {
        String text = pdfService.extractText(file);
        return ResponseEntity.ok(Collections.singletonMap("text", text));
    }

    @PostMapping("/render")
    public ResponseEntity<byte[]> renderPdf(@RequestBody JsonNode cvData) {
        byte[] pdfBytes = pdfService.renderCv(cvData);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=EuroCV.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
