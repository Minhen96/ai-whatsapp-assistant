package com.mh.AIAssistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "knowledge_entries")
public class KnowledgeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    // store as PostgreSQL array
    @Column(
        name = "embedding",
        columnDefinition = "double precision[]"
    )
    private Double[] embedding;

    private LocalDateTime createdAt = LocalDateTime.now();

    public KnowledgeEntry() {}

    public KnowledgeEntry(String userId, String content, Double[] embedding) {
        this.userId = userId;
        this.content = content;
        this.embedding = embedding;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Double[] getEmbedding() { return embedding; }
    public void setEmbedding(Double[] embedding) { this.embedding = embedding; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
