package com.mh.AIAssistant.repository;

import com.mh.AIAssistant.model.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeEntry, Long> {
    List<KnowledgeEntry> findByUserId(String userId);
}
