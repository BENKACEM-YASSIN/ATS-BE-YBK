package com.ats.optimizer.controller;

import com.ats.optimizer.model.dto.JobSearchRequest;
import com.ats.optimizer.model.dto.JobSearchResponse;
import com.ats.optimizer.service.JobSearchService;
import com.ats.optimizer.service.ProfileService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class JobSearchController {

    private final JobSearchService jobSearchService;
    private final ProfileService profileService;

    @PostMapping("/search")
    public ResponseEntity<?> searchJobs(@RequestBody JobSearchRequest request) {
        try {
            log.info("Job search request received: location={}, jobTitle={}, maxResults={}", 
                request.getLocation(), request.getJobTitle(), request.getMaxResults());
            
            // Get CV profile JSON for scoring
            JsonNode cvProfile = profileService.getLatestProfile();
            String cvProfileJson = cvProfile != null ? cvProfile.toString() : null;
            
            JobSearchResponse response = jobSearchService.searchJobs(request, cvProfileJson);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in job search", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to search jobs", "message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchJobsGet(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(defaultValue = "24") Integer maxResults) {
        try {
            JobSearchRequest request = new JobSearchRequest();
            request.setLocation(location);
            request.setJobTitle(jobTitle);
            request.setMaxResults(maxResults);
            
            JsonNode cvProfile = profileService.getLatestProfile();
            String cvProfileJson = cvProfile != null ? cvProfile.toString() : null;
            
            JobSearchResponse response = jobSearchService.searchJobs(request, cvProfileJson);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to search jobs", "message", e.getMessage()));
        }
    }
}
