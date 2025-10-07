package com.mh.AIAssistant.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.mh.AIAssistant.service.DeepSeekAIService;
import com.mh.AIAssistant.service.FileStorageService;
import com.mh.AIAssistant.service.OcrService;
import com.mh.AIAssistant.service.OpenAIEmbeddingService;
import com.mh.AIAssistant.service.DocumentService;
import com.mh.AIAssistant.repository.KnowledgeBaseRepository;
import com.mh.AIAssistant.dto.DocumentInfo;
import com.mh.AIAssistant.model.KnowledgeEntry;
import com.mh.AIAssistant.websocket.WebSocketService;
import com.mh.AIAssistant.service.WhatsappService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import net.sourceforge.tess4j.TesseractException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private DeepSeekAIService deepSeekAIService;

    @Autowired
    private OpenAIEmbeddingService embeddingService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private WhatsappService whatsappService;

    @Autowired
    private DocumentService documentService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            String mode = request.get("mode");
            String userId = request.get("userId");

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty"));
            }

            Map<String, Object> result = new HashMap<>();
            
            if ("store".equals(mode)) {
                whatsappService.storeTextAndEmbed(userId, message);
                result.put("response", "✅ Message stored successfully in knowledge base!");
                result.put("mode", mode);
                result.put("userId", userId);
            } else {
                // Chat mode - get response with document references
                String response = whatsappService.chatReply(userId, message);
                
                // Find relevant documents
                List<DocumentInfo> documents = 
                    documentService.findRelevantDocuments(userId, message);
                
                result.put("response", response);
                result.put("mode", mode);
                result.put("userId", userId);
                result.put("documents", documents);
                result.put("hasDocuments", !documents.isEmpty());
            }

            // Notify other frontend clients
            webSocketService.notifyFrontendMessage(userId, message, (String) result.get("response"));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error in /chat endpoint", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to process request: " + e.getMessage()));
        }
    }

    @GetMapping("/document/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @RequestParam("userId") String userId) {
        try {
            logger.info("Download request for document ID: {} by user: {}", id, userId);

            // Get document info
            var docOpt = documentService.getDocumentById(id);
            if (docOpt.isEmpty()) {
                logger.warn("Document not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            DocumentInfo doc = docOpt.get();

            // Verify user owns this document
            KnowledgeEntry entry = knowledgeBaseRepository.findById(id).orElse(null);
            if (entry == null || !entry.getUserId().equals(userId)) {
                logger.warn("Unauthorized access attempt to document: {} by user: {}", id, userId);
                return ResponseEntity.status(403).build();
            }

            // Check if document has a file
            if (!doc.isHasFile() || doc.getFilePath() == null) {
                logger.warn("Document has no associated file: {}", id);
                return ResponseEntity.badRequest().build();
            }

            // Verify file exists
            if (!documentService.fileExists(doc.getFilePath())) {
                logger.error("File not found on disk: {}", doc.getFilePath());
                return ResponseEntity.notFound().build();
            }

            // Load file as Resource
            Resource resource = documentService.getFileResource(doc.getFilePath());

            // Determine content type
            String contentType = doc.getFileType() != null ? 
                doc.getFileType() : "application/octet-stream";

            // Encode filename for Content-Disposition header
            String encodedFileName = URLEncoder.encode(
                doc.getFileName() != null ? doc.getFileName() : "download", 
                StandardCharsets.UTF_8
            ).replaceAll("\\+", "%20");

            logger.info("Serving file: {} ({})", doc.getFileName(), contentType);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);

        } catch (Exception e) {
            logger.error("Error downloading document: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/knowledge/store")
    public ResponseEntity<Map<String, String>> storeKnowledge(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "text", required = false) String text) {
        try {
            logger.info("Store knowledge request - userId: {}, hasFile: {}, hasText: {}", 
                userId, file != null && !file.isEmpty(), text != null && !text.trim().isEmpty());

            StringBuilder aggregated = new StringBuilder();

            if (text != null && !text.trim().isEmpty()) {
                logger.info("Storing text: {} characters", text.length());
                whatsappService.storeTextAndEmbed(userId, text.trim(), null, null, null);
                aggregated.append(text.trim()).append("\n");
            }

            if (file != null && !file.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                logger.info("Processing file: {}", originalFilename);
                
                if (!ocrService.isSupported(originalFilename)) {
                    logger.warn("Unsupported file type: {}", originalFilename);
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Unsupported file type: " + originalFilename));
                }
                
                String fileName = userId + "_" + System.currentTimeMillis() + "_" + originalFilename;
                String savedPath = fileStorageService.saveMultipartFile(file, fileName);

                if (savedPath != null) {
                    File savedFile = new File(savedPath);
                    String extractedText = "";
                    
                    try {
                        extractedText = ocrService.extractText(savedFile);
                        logger.info("Extracted {} characters from file", extractedText.length());
                        
                    } catch (Exception e) {
                        logger.error("Text extraction failed for file: {}", originalFilename, e);
                        return ResponseEntity.internalServerError()
                            .body(Map.of("error", "Failed to extract text from file: " + e.getMessage()));
                    }

                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        whatsappService.storeTextAndEmbed(
                            userId, 
                            extractedText,
                            savedPath,
                            originalFilename,
                            file.getContentType()
                        );
                        aggregated.append(extractedText).append("\n");
                        logger.info("Successfully stored file content with metadata in knowledge base");
                    } else {
                        logger.warn("No text could be extracted from file: {}", originalFilename);
                        return ResponseEntity.ok(Map.of(
                            "message", "File uploaded but no text could be extracted",
                            "userId", userId
                        ));
                    }
                }
            }

            if (aggregated.length() == 0) {
                logger.warn("No content to store - neither text nor file provided");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nothing to store: provide text and/or file"));
            }

            Map<String, String> result = new HashMap<>();
            result.put("message", "✅ Knowledge stored successfully!");
            result.put("userId", userId);
            result.put("charactersStored", String.valueOf(aggregated.length()));
            
            logger.info("Successfully stored knowledge for user {}: {} characters", userId, aggregated.length());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Unexpected error storing knowledge", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mode") String mode,
            @RequestParam("userId") String userId) {
        return storeKnowledge(file, userId, null);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "AI Assistant Backend");
        return ResponseEntity.ok(result);
    }
}