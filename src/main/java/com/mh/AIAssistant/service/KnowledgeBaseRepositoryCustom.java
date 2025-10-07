package com.mh.AIAssistant.service;

import com.mh.AIAssistant.model.KnowledgeEntry;
import com.mh.AIAssistant.repository.KnowledgeBaseRepository;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class KnowledgeBaseRepositoryCustom {

    private final KnowledgeBaseRepository repository;

    public KnowledgeBaseRepositoryCustom(KnowledgeBaseRepository repository) {
        this.repository = repository;
    }

    /**
     * Find similar entries and convert List<Double> to PostgreSQL array format
     */
    public List<KnowledgeEntry> findSimilarEntries(List<Double> queryEmbedding, String userId, int limit) {
        String embeddingStr = queryEmbedding.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",", "{", "}"));
        
        List<Object[]> rawResults = repository.findSimilarEntriesRaw(embeddingStr, userId, limit);
        
        List<KnowledgeEntry> results = new ArrayList<>();
        for (Object[] row : rawResults) {
            KnowledgeEntry entry = new KnowledgeEntry();
            entry.setId(((Number) row[0]).longValue());
            entry.setUserId((String) row[1]);
            entry.setContent((String) row[2]);
            
            if (row[3] != null) {
                entry.setEmbedding((Double[]) row[3]);
            }
            
            if (row[4] != null) {
                if (row[4] instanceof Timestamp) {
                    entry.setCreatedAt(((Timestamp) row[4]).toLocalDateTime());
                } else if (row[4] instanceof LocalDateTime) {
                    entry.setCreatedAt((LocalDateTime) row[4]);
                }
            }
            
            if (row[5] != null) {
                entry.setSimilarityScore(((Number) row[5]).doubleValue());
            }
            
            // Add file metadata (indices 6, 7, 8)
            if (row.length > 6 && row[6] != null) {
                entry.setFilePath((String) row[6]);
            }
            if (row.length > 7 && row[7] != null) {
                entry.setFileName((String) row[7]);
            }
            if (row.length > 8 && row[8] != null) {
                entry.setFileType((String) row[8]);
            }
            
            results.add(entry);
        }
        
        return results;
    }
}