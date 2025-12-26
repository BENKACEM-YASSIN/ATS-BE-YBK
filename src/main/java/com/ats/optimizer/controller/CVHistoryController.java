package com.ats.optimizer.controller;

import com.ats.optimizer.entity.CVHistory;
import com.ats.optimizer.service.CVHistoryService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4300"})
public class CVHistoryController {
    @Autowired
    private CVHistoryService service;

    @GetMapping
    public List<CVHistory> getAll() {
        return service.getAll();
    }

    @PostMapping
    public ResponseEntity<CVHistory> save(@RequestParam String title, @RequestBody JsonNode cvData) {
        if (service.existsTitle(title)) {
            return ResponseEntity.status(409).build();
        }
        return ResponseEntity.ok(service.save(title, cvData));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CVHistory> update(@PathVariable Long id, @RequestBody JsonNode cvData) {
        CVHistory h = service.update(id, cvData);
        if (h != null) return ResponseEntity.ok(h);
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean ok = service.delete(id);
        if (ok) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<CVHistory> pin(@PathVariable Long id, @RequestParam boolean pinned) {
        CVHistory h = service.togglePin(id, pinned);
        if (h != null) return ResponseEntity.ok(h);
        return ResponseEntity.notFound().build();
    }
}
