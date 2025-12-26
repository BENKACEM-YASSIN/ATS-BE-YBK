package com.ats.optimizer.service;

import com.ats.optimizer.entity.CVHistory;
import com.ats.optimizer.repository.CVHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CVHistoryService {
    @Autowired
    private CVHistoryRepository repo;

    public List<CVHistory> getAll() {
        return repo.findAllByOrderByIdDesc();
    }

    public CVHistory save(String title, JsonNode cvData) {
        CVHistory h = new CVHistory();
        h.setTitle(title);
        h.setCvDataJson(cvData.toString());
        return repo.save(h);
    }

    public CVHistory update(Long id, JsonNode cvData) {
        CVHistory h = repo.findById(id).orElse(null);
        if (h == null) return null;
        h.setCvDataJson(cvData.toString());
        return repo.save(h);
    }

    public boolean delete(Long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }

    public boolean existsTitle(String title) {
        return repo.existsByTitle(title);
    }

    public CVHistory togglePin(Long id, boolean pin) {
        CVHistory h = repo.findById(id).orElse(null);
        if (h == null) return null;
        h.setPinned(pin);
        return repo.save(h);
    }
}
