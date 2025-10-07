package com.mh.AIAssistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_entries")
public class KnowledgeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(
        name = "embedding",
        columnDefinition = "double precision[]"
    )
    private Double[] embedding;

    private LocalDateTime createdAt = LocalDateTime.now();

    // New fields for file metadata
    @Column(name = "file_path")
    private String filePath;  // Store the actual file path on server

    @Column(name = "file_name")
    private String fileName;  // Original filename

    @Column(name = "file_type")
    private String fileType;  // mime type or extension

    @Transient
    private Double similarityScore;

    public KnowledgeEntry() {}

    public KnowledgeEntry(String userId, String content, Double[] embedding) {
        this.userId = userId;
        this.content = content;
        this.embedding = embedding;
    }

    // Add constructor with file metadata
    public KnowledgeEntry(String userId, String content, Double[] embedding, 
                         String filePath, String fileName, String fileType) {
        this.userId = userId;
        this.content = content;
        this.embedding = embedding;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    // All existing getters/setters...
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

    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }

    // New getters/setters for file metadata
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    // Helper method to check if this entry has an associated file
    public boolean hasFile() {
        return filePath != null && !filePath.isEmpty();
    }
}