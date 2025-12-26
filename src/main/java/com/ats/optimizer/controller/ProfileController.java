package com.ats.optimizer.controller;

import com.ats.optimizer.service.ProfileService;
import com.ats.optimizer.model.dto.ProfileDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    public ResponseEntity<Void> saveProfile(@RequestBody JsonNode cvData) {
        profileService.saveProfile(cvData);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping
    public ResponseEntity<Void> updateProfileDetails(@RequestBody ProfileDTO update) {
        profileService.updateProfileDetails(update);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/latest")
    public ResponseEntity<JsonNode> getLatestProfile() {
        JsonNode profile = profileService.getLatestProfile();
        if (profile != null) {
            return ResponseEntity.ok(profile);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<ProfileDTO> getProfileDashboard() {
        return ResponseEntity.ok(profileService.getProfileDashboard());
    }
}
