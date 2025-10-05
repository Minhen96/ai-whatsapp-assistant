package com.mh.AIAssistant.controller; 

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.mh.AIAssistant.service.FileStorageService;
import com.mh.AIAssistant.service.WhatsappService;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/whatsapp")
public class WhatsappController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappController.class);

    @Resource
    private FileStorageService fileStorageService;
    
    @Resource
    private WhatsappService whatsappService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello AI!";
    }
    
    // ✅ Test endpoint to verify service injection
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "ok");
        result.put("whatsappService", whatsappService != null ? "injected" : "NULL");
        result.put("fileStorageService", fileStorageService != null ? "injected" : "NULL");
        return ResponseEntity.ok(result);
    }
    
    // ✅ Minimal test endpoint for incoming_manual
    @PostMapping(value = "/incoming_manual_test", 
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> testManual(@RequestParam Map<String,String> params) {
        Map<String, String> result = new HashMap<>();
        result.put("received_params", params.toString());
        result.put("from", params.getOrDefault("from", "missing"));
        result.put("body", params.getOrDefault("body", "missing"));
        result.put("whatsappService", whatsappService != null ? "available" : "NULL");
        return ResponseEntity.ok(result);
    }

    // ✅ Twilio webhook - Returns TwiML XML
    @PostMapping(value = "/incoming", produces = MediaType.APPLICATION_XML_VALUE)
    public String receiveMessage(@RequestParam("From") String from,
                                 @RequestParam("Body") String body) {
        logger.info("Twilio message from {}: {}", from, body);

        try {
            // Process message through WhatsappService
            Map<String, String> params = new HashMap<>();
            params.put("From", from);
            params.put("Body", body);
            
            // Don't send via Twilio here - we return TwiML instead
            String response = whatsappService.handleIncoming(params, false);
            
            // Return TwiML response for Twilio
            return twiml(response);
            
        } catch (Exception e) {
            logger.error("Error processing Twilio message", e);
            return twiml("Sorry, I encountered an error processing your message.");
        }
    }

    // ✅ Twilio webhook with media support - Returns TwiML XML
    @PostMapping(value = "/incoming_2", produces = MediaType.APPLICATION_XML_VALUE)
    public String receiveMessage2(@RequestParam Map<String,String> params) {
        String from = params.get("From");                 // e.g. "whatsapp:+60123..."
        String body = params.getOrDefault("Body", "");
        int numMedia = Integer.parseInt(params.getOrDefault("NumMedia", "0"));

        logger.info("Twilio incoming From={} Body={} NumMedia={}", from, body, numMedia);

        try {
            if (numMedia > 0) {
                // Handle media message
                String mediaUrl = params.get("MediaUrl0");
                String fileName = from.replace(":", "_") + "_" + System.currentTimeMillis();
                String savedPath = fileStorageService.saveFile(mediaUrl, fileName);
                logger.info("File saved: {}", savedPath);
                
                // You might want to process the file through WhatsappService too
                return twiml("✅ I saved your file and it's now in the knowledge base!");
                
            } else {
                // Handle text message
                String response = whatsappService.handleIncoming(params);
                return twiml(response);
            }
        } catch (Exception e) {
            logger.error("Error processing Twilio message with media", e);
            return twiml("Sorry, I couldn't process your message.");
        }
    }
    
    // ✅ Frontend manual trigger - Returns JSON
    @PostMapping(value = "/incoming_manual", 
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> replyMessage(@RequestParam Map<String,String> params) {
        try {
            logger.info("Received frontend manual message with params: {}", params);
            
            String from = params.getOrDefault("from", "frontend-user");
            String body = params.getOrDefault("body", "");
            
            // Validation
            if (body == null || body.trim().isEmpty()) {
                logger.warn("Empty message body received from {}", from);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Message body cannot be empty");
                errorResponse.put("userId", from);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            logger.info("Processing manual message from {}: {}", from, body);
            
            // Normalize params for WhatsappService (use uppercase keys like Twilio)
            Map<String, String> normalizedParams = new HashMap<>();
            normalizedParams.put("From", from);
            normalizedParams.put("Body", body);
            
            // Handle the incoming message and get response
            String response = whatsappService.handleIncoming(normalizedParams);
            
            logger.info("WhatsappService returned response: {}", response);
            
            Map<String, String> result = new HashMap<>();
            result.put("response", response);
            result.put("userId", from);
            result.put("mode", "whatsapp");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error processing manual message", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process message: " + e.getMessage());
            errorResponse.put("details", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // Global exception handler for this controller
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("Unhandled exception in WhatsappController", e);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal server error");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("type", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // Helper method to generate TwiML XML response for Twilio
    private String twiml(String message) {
        // Escape XML special characters
        String escaped = message
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
            
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<Response><Message>" + escaped + "</Message></Response>";
    }
}