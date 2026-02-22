package com.ats.optimizer.repository;

import com.ats.optimizer.entity.CVHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CVHistoryRepository extends JpaRepository<CVHistory, Long> {
    List<CVHistory> findAllByOrderByIdDesc();
    boolean existsByTitle(String title);
    CVHistory findByTitle(String title);
    List<CVHistory> findByCvDocumentIsNullAndCvDataJsonIsNotNull();
}
