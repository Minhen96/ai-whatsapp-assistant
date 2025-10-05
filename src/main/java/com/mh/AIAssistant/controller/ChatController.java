package com.mh.AIAssistant.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.mh.AIAssistant.service.DeepSeekAIService;
// import removed: DeepSeekEmbeddingService (we reuse OpenAIEmbeddingService for embeddings)
import com.mh.AIAssistant.service.FileStorageService;
import com.mh.AIAssistant.service.OcrService;
import com.mh.AIAssistant.service.OpenAIEmbeddingService;
import com.mh.AIAssistant.repository.KnowledgeBaseRepository;
import com.mh.AIAssistant.model.KnowledgeEntry;
import com.mh.AIAssistant.websocket.WebSocketService;
import com.mh.AIAssistant.service.WhatsappService;

import jakarta.annotation.Resource;
import net.sourceforge.tess4j.TesseractException;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class ChatController {

    @Resource
    private DeepSeekAIService deepSeekAIService;

    @Resource
    private OpenAIEmbeddingService embeddingService;

    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private OcrService ocrService;

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private WebSocketService webSocketService;

    @Resource
    private WhatsappService whatsappService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            String mode = request.get("mode");
            String userId = request.get("userId");

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty"));
            }

            String response;
            
            if ("store".equals(mode)) {
                // Reuse WhatsappService logic to keep exact behavior in both channels
                whatsappService.storeTextAndEmbed(userId, message);
                response = "✅ Message stored successfully in knowledge base!";
            } else {
                // Chat mode - use same reply generation as WA
                response = whatsappService.chatReply(userId, message);
            }

            Map<String, String> result = new HashMap<>();
            result.put("response", response);
            result.put("mode", mode);
            result.put("userId", userId);

            // Notify other frontend clients about the message
            webSocketService.notifyFrontendMessage(userId, message, response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to process request: " + e.getMessage()));
        }
    }

    // Store knowledge from optional text and/or optional file (supports both in one call)
    @PostMapping("/knowledge/store")
    public ResponseEntity<Map<String, String>> storeKnowledge(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "text", required = false) String text) {
        try {
            StringBuilder aggregated = new StringBuilder();

            // 1) Optional raw text (frontend text box)
            if (text != null && !text.trim().isEmpty()) {
                whatsappService.storeTextAndEmbed(userId, text.trim());
                aggregated.append(text.trim()).append("\n");
            }

            // 2) Optional file (OCR -> text -> embed)
            if (file != null && !file.isEmpty()) {
                String fileName = userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String savedPath = fileStorageService.saveMultipartFile(file, fileName);

                if (savedPath != null) {
                    File savedFile = new File(savedPath);
                    String extractedText = "";
                    try {
                        extractedText = ocrService.extractText(savedFile);
                    } catch (TesseractException e) {
                        e.printStackTrace();
                        extractedText = "[OCR extraction failed]";
                    }

                    if (!extractedText.trim().isEmpty()) {
                        whatsappService.storeTextAndEmbed(userId, extractedText);
                        aggregated.append(extractedText).append("\n");
                    }
                }
            }

            if (aggregated.length() == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nothing to store: provide text and/or file"));
            }

            Map<String, String> result = new HashMap<>();
            result.put("message", "✅ Knowledge stored successfully!");
            result.put("userId", userId);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to store knowledge: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mode") String mode,
            @RequestParam("userId") String userId) {
        // Delegate to unified store endpoint (file-only request)
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
