package com.ats.optimizer.service;

import com.ats.optimizer.entity.CVHistory;
import com.ats.optimizer.entity.UserProfile;
import com.ats.optimizer.repository.CVHistoryRepository;
import com.ats.optimizer.repository.ProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CVDataNormalizationMigration {

    private final CVHistoryRepository cvHistoryRepository;
    private final ProfileRepository profileRepository;
    private final CVDocumentMapper cvDocumentMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateLegacyJsonStorage() {
        migrateHistory();
        migrateProfiles();
    }

    private void migrateHistory() {
        List<CVHistory> legacyRows = cvHistoryRepository.findByCvDocumentIsNullAndCvDataJsonIsNotNull();
        int migrated = 0;
        for (CVHistory row : legacyRows) {
            JsonNode legacyJson = cvDocumentMapper.parseRawJson(row.getCvDataJson());
            if (legacyJson == null) continue;
            row.setCvDocument(cvDocumentMapper.fromJson(legacyJson));
            row.setCvDataJson(null);
            migrated++;
        }
        if (migrated > 0) {
            cvHistoryRepository.saveAll(legacyRows);
            log.info("Migrated {} legacy cv_history rows to normalized CV tables.", migrated);
        }
    }

    private void migrateProfiles() {
        List<UserProfile> legacyRows = profileRepository.findByCvDocumentIsNullAndCvDataJsonIsNotNull();
        int migrated = 0;
        for (UserProfile row : legacyRows) {
            JsonNode legacyJson = cvDocumentMapper.parseRawJson(row.getCvDataJson());
            if (legacyJson == null) continue;
            row.setCvDocument(cvDocumentMapper.fromJson(legacyJson));
            row.setCvDataJson(null);
            migrated++;
        }
        if (migrated > 0) {
            profileRepository.saveAll(legacyRows);
            log.info("Migrated {} legacy user_profiles rows to normalized CV tables.", migrated);
        }
    }
}

