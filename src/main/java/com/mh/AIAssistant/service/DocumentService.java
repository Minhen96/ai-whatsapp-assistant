package com.mh.AIAssistant.service;

import com.mh.AIAssistant.dto.DocumentInfo;
import com.mh.AIAssistant.model.KnowledgeEntry;
import com.mh.AIAssistant.repository.KnowledgeBaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    private static final double SIMILARITY_THRESHOLD = 0.3;
    
    @jakarta.annotation.Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    @jakarta.annotation.Resource
    private OpenAIEmbeddingService embeddingService;

    /**
     * Find relevant documents based on query with dynamic filtering
     */
    public List<DocumentInfo> findRelevantDocuments(String userId, String query) {
        try {
            // Get query embedding
            List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                logger.warn("Failed to generate embedding for query");
                return Collections.emptyList();
            }

            // Convert embedding to PostgreSQL array string
            String embeddingStr = queryEmbedding.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "{", "}"));

            // Get top 10 similar entries
            List<Object[]> rawResults = knowledgeBaseRepository.findSimilarEntriesRaw(
                    embeddingStr, userId, 50 // increase a bit to handle filtering
            );

            Map<String, DocumentInfo> uniqueDocs = new HashMap<>();
            for (Object[] row : rawResults) {
                try {
                    Long id = ((Number) row[0]).longValue();
                    String content = (String) row[2];
                    Double similarity = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
                    String filePath = (String) row[6];
                    String fileName = (String) row[7];
                    String fileType = (String) row[8];

                    if (similarity < SIMILARITY_THRESHOLD) continue;

                    // Unique key based on file path or file name
                    String uniqueKey = (filePath != null && !filePath.isEmpty())
                            ? filePath
                            : (fileName != null ? fileName : String.valueOf(id));

                    // Keep only the one with highest similarity
                    DocumentInfo existing = uniqueDocs.get(uniqueKey);
                    if (existing == null || similarity > existing.getSimilarity()) {
                        DocumentInfo doc = new DocumentInfo();
                        doc.setId(id);
                        doc.setContent(content);
                        doc.setSimilarity(similarity);
                        doc.setFilePath(filePath);
                        doc.setFileName(fileName);
                        doc.setFileType(fileType);
                        doc.setHasFile(filePath != null && !filePath.isEmpty());

                        uniqueDocs.put(uniqueKey, doc);
                    }

                } catch (Exception e) {
                    logger.error("Error processing result row", e);
                }
            }

            List<DocumentInfo> documents = new ArrayList<>(uniqueDocs.values());
            documents.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity())); // sort descending

            logger.info("Found {} unique relevant documents above threshold {}",
                    documents.size(), SIMILARITY_THRESHOLD);

            return documents.stream().limit(10).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error finding relevant documents", e);
            return Collections.emptyList();
        }
    }


    /**
     * Get file resource for download
     */
    public Resource getFileResource(String filePath) throws MalformedURLException {
        Path path = Paths.get(filePath);
        Resource resource = new UrlResource(path.toUri());
        
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("File not found or not readable: " + filePath);
        }
    }

    /**
     * Verify file exists
     */
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        File file = new File(filePath);
        return file.exists() && file.isFile();
    }

    /**
     * Get file info by knowledge entry ID
     */
    public Optional<DocumentInfo> getDocumentById(Long id) {
        Optional<KnowledgeEntry> entryOpt = knowledgeBaseRepository.findById(id);
        
        if (entryOpt.isEmpty()) {
            return Optional.empty();
        }

        KnowledgeEntry entry = entryOpt.get();
        DocumentInfo doc = new DocumentInfo();
        doc.setId(entry.getId());
        doc.setContent(entry.getContent());
        doc.setFilePath(entry.getFilePath());
        doc.setFileName(entry.getFileName());
        doc.setFileType(entry.getFileType());
        doc.setHasFile(entry.hasFile());

        return Optional.of(doc);
    }
}