package com.ats.optimizer.service;

import com.ats.optimizer.entity.UserProfile;
import com.ats.optimizer.model.dto.ProfileDTO;
import com.ats.optimizer.repository.CVHistoryRepository;
import com.ats.optimizer.repository.ProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final CVHistoryRepository cvHistoryRepository;
    private final ObjectMapper objectMapper;

    public void saveProfile(JsonNode cvData) {
        UserProfile profile = profileRepository.findTopByOrderByIdDesc();
        if (profile == null) {
            profile = new UserProfile();
            profile.setProfileProgress(20); // Initial progress
        }
        
        profile.setCvDataJson(cvData.toString());
        profile.setLastEdited(LocalDateTime.now());
        
        // Extract basic info from JSON if available
        if (cvData.has("personalInfo")) {
            JsonNode info = cvData.get("personalInfo");
            if (info.has("firstName")) profile.setFirstName(info.get("firstName").asText());
            if (info.has("lastName")) profile.setLastName(info.get("lastName").asText());
            if (info.has("title")) profile.setTitle(info.get("title").asText());
            if (info.has("email")) profile.setEmail(info.get("email").asText());
        }
        
        profileRepository.save(profile);
    }

    public JsonNode getLatestProfile() {
        UserProfile profile = profileRepository.findTopByOrderByIdDesc();
        if (profile != null && profile.getCvDataJson() != null) {
            try {
                return objectMapper.readTree(profile.getCvDataJson());
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse stored profile JSON");
            }
        }
        return null;
    }
    
    public ProfileDTO getProfileDashboard() {
        UserProfile profile = profileRepository.findTopByOrderByIdDesc();
        if (profile == null) {
            // Return dummy/empty data
            ProfileDTO dto = new ProfileDTO();
            dto.setFirstName("Guest");
            dto.setLastName("User");
            dto.setProfileProgress(0);
            return dto;
        }
        
        // Update visit time
        profile.setLastVisit(LocalDateTime.now());
        profileRepository.save(profile);
        
        ProfileDTO dto = new ProfileDTO();
        dto.setFirstName(profile.getFirstName());
        dto.setLastName(profile.getLastName());
        dto.setTitle(profile.getTitle());
        dto.setEmail(profile.getEmail());
        dto.setMotto(profile.getMotto());
        dto.setSummary(profile.getSummary());
        dto.setPhotoUrl(profile.getPhotoUrl());
        dto.setDrivingLicence(profile.getDrivingLicence());
        dto.setHobbies(profile.getHobbies());
        dto.setInterests(profile.getInterests());
        dto.setProfileProgress(profile.getProfileProgress());
        dto.setLastVisit(profile.getLastVisit());
        dto.setLastEditedDate(profile.getLastEdited());
        dto.setLastEditedFileName(profile.getLastEditedFileName());
        
        // Stats
        dto.setCvCount(cvHistoryRepository.count());
        
        // Calculate detailed stats from the JSON if possible
        try {
            if (profile.getCvDataJson() != null) {
                JsonNode root = objectMapper.readTree(profile.getCvDataJson());
                
                if (root.has("workExperience")) {
                    dto.setWorkExperienceCount(root.get("workExperience").size());
                    // Rough estimation of years could be done here
                }
                if (root.has("education")) {
                    dto.setEducationCount(root.get("education").size());
                }
                if (root.has("skills")) {
                    JsonNode skills = root.get("skills");
                    int count = 0;
                    if (skills.has("digitalSkills")) count += skills.get("digitalSkills").size();
                    if (skills.has("softSkills")) count += skills.get("softSkills").size();
                    dto.setSkillsCount(count);
                    
                    if (skills.has("otherLanguages")) {
                         dto.setLanguagesCount(skills.get("otherLanguages").size() + 1); // +1 for mother tongue
                    }
                }
            }
        } catch (Exception e) {
            // Ignore parse errors for stats
        }
        
        return dto;
    }
    
    public void updateProfileDetails(ProfileDTO update) {
        UserProfile profile = profileRepository.findTopByOrderByIdDesc();
        if (profile == null) {
            profile = new UserProfile();
        }
        
        if (update.getMotto() != null) profile.setMotto(update.getMotto());
        if (update.getSummary() != null) profile.setSummary(update.getSummary());
        if (update.getFirstName() != null) profile.setFirstName(update.getFirstName());
        if (update.getLastName() != null) profile.setLastName(update.getLastName());
        if (update.getPhotoUrl() != null) profile.setPhotoUrl(update.getPhotoUrl());
        if (update.getDrivingLicence() != null) profile.setDrivingLicence(update.getDrivingLicence());
        if (update.getHobbies() != null) profile.setHobbies(update.getHobbies());
        if (update.getInterests() != null) profile.setInterests(update.getInterests());
        
        // Update progress logic
        int progress = 20;
        if (profile.getSummary() != null && !profile.getSummary().isEmpty()) progress += 10;
        if (profile.getMotto() != null && !profile.getMotto().isEmpty()) progress += 5;
        if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) progress += 10;
        if (profile.getDrivingLicence() != null && !profile.getDrivingLicence().isEmpty()) progress += 5;
        if (profile.getHobbies() != null && !profile.getHobbies().isEmpty()) progress += 5;
        if (profile.getInterests() != null && !profile.getInterests().isEmpty()) progress += 5;
        // Check CV data presence
        if (profile.getCvDataJson() != null) progress += 40;
        
        profile.setProfileProgress(Math.min(progress, 100));
        
        profileRepository.save(profile);
    }
}
