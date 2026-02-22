package com.ats.optimizer.service;

import com.ats.optimizer.entity.CVHistory;
import com.ats.optimizer.repository.CVHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CVHistoryService {

    private final CVHistoryRepository repo;
    private final CVDocumentMapper cvDocumentMapper;

    @Transactional(readOnly = true)
    public List<CVHistory> getAll() {
        List<CVHistory> items = repo.findAllByOrderByIdDesc();
        items.forEach(this::attachCvDataPayload);
        return items;
    }

    public CVHistory save(String title, JsonNode cvData) {
        CVHistory history = new CVHistory();
        history.setTitle(title);
        history.setCvDocument(cvDocumentMapper.fromJson(cvData));
        history.setCvDataJson(null);
        CVHistory saved = repo.save(history);
        attachCvDataPayload(saved);
        return saved;
    }

    public CVHistory update(Long id, JsonNode cvData) {
        CVHistory history = repo.findById(id).orElse(null);
        if (history == null) return null;
        history.setCvDocument(cvDocumentMapper.fromJson(cvData));
        history.setCvDataJson(null);
        CVHistory saved = repo.save(history);
        attachCvDataPayload(saved);
        return saved;
    }

    public boolean delete(Long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean existsTitle(String title) {
        return repo.existsByTitle(title);
    }

    public CVHistory togglePin(Long id, boolean pin) {
        CVHistory history = repo.findById(id).orElse(null);
        if (history == null) return null;
        history.setPinned(pin);
        CVHistory saved = repo.save(history);
        attachCvDataPayload(saved);
        return saved;
    }

    private void attachCvDataPayload(CVHistory history) {
        if (history == null) return;

        if (history.getCvDocument() != null) {
            history.setCvData(cvDocumentMapper.toJson(history.getCvDocument()));
            history.setCvDataJson(null);
            return;
        }

        JsonNode legacy = cvDocumentMapper.parseRawJson(history.getCvDataJson());
        history.setCvData(legacy);
    }
}

