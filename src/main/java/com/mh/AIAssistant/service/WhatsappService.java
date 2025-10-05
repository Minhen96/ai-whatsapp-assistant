package com.mh.AIAssistant.service;

import com.mh.AIAssistant.configuration.TwilioConfig;
import com.mh.AIAssistant.enums.UserMode;
import com.mh.AIAssistant.model.KnowledgeEntry;
import com.mh.AIAssistant.repository.KnowledgeBaseRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class WhatsappService {

    private final TwilioConfig twilioConfig;
    private final FileStorageService fileStorageService;
    private final OcrService ocrService;
    private final OpenAIEmbeddingService embeddingService;
    private final DeepSeekAIService deepSeekAIService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    // simple in-memory session
    private final Map<String, UserMode> userSessions = new HashMap<>();

    // constructor injection
    public WhatsappService(
            TwilioConfig twilioConfig,
            FileStorageService fileStorageService,
            OcrService ocrService,
            OpenAIEmbeddingService embeddingService,
            DeepSeekAIService deepSeekAIService,
            KnowledgeBaseRepository knowledgeBaseRepository
    ) {
        this.twilioConfig = twilioConfig;
        this.fileStorageService = fileStorageService;
        this.ocrService = ocrService;
        this.embeddingService = embeddingService;
        this.deepSeekAIService = deepSeekAIService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;

        Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
    }

    /**
     * Entry point: process incoming WA message
     * Returns reply string - caller decides whether to send via Twilio
     */
    public String handleIncoming(Map<String, String> params) {
        return handleIncoming(params, false);
    }

    /**
     * Overloaded version with sendViaTwilio flag
     * @param params - message parameters
     * @param sendViaTwilio - if true, automatically send reply via Twilio WhatsApp
     */
    public String handleIncoming(Map<String, String> params, boolean sendViaTwilio) {
        String from = params.get("From");
        String body = params.getOrDefault("Body", "").trim();
        int numMedia = Integer.parseInt(params.getOrDefault("NumMedia", "0"));

        // init session
        userSessions.putIfAbsent(from, UserMode.NONE);
        UserMode mode = userSessions.get(from);

        String reply;

        if ("end".equalsIgnoreCase(body)) {
            reply = handleEndCommand(from);
            if (sendViaTwilio) {
                sendMessage(from, reply);
            }
            return reply;
        }

        switch (mode) {
            case NONE:
                reply = handleNoneMode(from, body);
                break;
            case STORE:
                reply = handleStoreMode(from, body, params, numMedia);
                break;
            case CHAT:
                reply = handleChatMode(from, body);
                break;
            default:
                reply = promptOptions();
        }

        // Only send via Twilio if explicitly requested (for webhook calls)
        if (sendViaTwilio) {
            sendMessage(from, reply);
        }
        
        return reply;
    }

    /**
     * Reusable helper for web ChatController: store plain text into docs + DB embeddings
     */
    public void storeTextAndEmbed(String userId, String text) throws IOException {
        if (text == null || text.isBlank()) return;
        // Mirror to docs folder (same artifact path as WA)
        String fileName = userId.replace(":", "_") + "_" + System.currentTimeMillis() + ".txt";
        fileStorageService.saveText(text, fileName);

        // Generate embedding and persist
        List<Double> embeddingList = embeddingService.generateEmbedding(text);
        Double[] embeddingArray = embeddingList.toArray(new Double[0]);
        knowledgeBaseRepository.save(new KnowledgeEntry(userId, text, embeddingArray));
    }

    /**
     * Reusable helper for web ChatController: produce AI reply using same knowledge retrieval as WA
     */
    public String chatReply(String userId, String body) {
        List<KnowledgeEntry> entries = knowledgeBaseRepository.findByUserId(userId);
        List<String> context = entries.stream().map(KnowledgeEntry::getContent).toList();
        
        // Pass userId for conversation history
        return deepSeekAIService.chatWithKnowledge(userId, body, context);
    }

    private String handleChatMode(String from, String body) {
        List<KnowledgeEntry> entries = knowledgeBaseRepository.findByUserId(from);
        List<String> context = entries.stream().map(KnowledgeEntry::getContent).toList();
        
        // Pass from (user ID) for conversation history
        String aiReply = deepSeekAIService.chatWithKnowledge(from, body, context);
        return "🤖 AI says: " + aiReply + "\n\n(Type 'end' to finish)";
    }

    /**
     * Clear chat history when session ends
     */
    private String handleEndCommand(String from) {
        userSessions.put(from, UserMode.NONE);
        deepSeekAIService.clearHistory(from);  // Clear conversation history
        return "✅ Session ended.\n\n" + promptOptions();
    }

    private String handleNoneMode(String from, String body) {
        if ("1".equals(body) || "store".equalsIgnoreCase(body)) {
            userSessions.put(from, UserMode.STORE);
            return "📥 Store mode activated. Send me the text or document you want to store. Type 'end' to finish.";
        } else if ("2".equals(body) || "chat".equalsIgnoreCase(body)) {
            userSessions.put(from, UserMode.CHAT);
            return "🤖 Chat mode activated. Ask me any question. Type 'end' to finish.";
        }
        return promptOptions();
    }

    private String handleStoreMode(String from, String body, Map<String, String> params, int numMedia) {
        try {
            StringBuilder textToStore = new StringBuilder();
    
            // Handle files/images
            if (numMedia > 0) {
                for (int i = 0; i < numMedia; i++) {
                    String mediaUrl = params.get("MediaUrl" + i);
                    String fileName = from.replace(":", "_") + "_" + System.currentTimeMillis() + "_" + i;
                    String savedPath = fileStorageService.saveFile(mediaUrl, fileName);
    
                    File file = new File(savedPath);
                    try {
                        String extractedText = ocrService.extractText(file);
                        textToStore.append(extractedText).append("\n");
                    } catch (TesseractException e) {
                        System.err.println("Tesseract OCR failed: " + e.getMessage());
                        textToStore.append("[OCR failed - file saved without text extraction]\n");
                    } catch (Error e) {
                        // Catch native library errors (like Invalid memory access)
                        System.err.println("OCR native library error: " + e.getMessage());
                        textToStore.append("[OCR unavailable - file saved without text extraction]\n");
                    } catch (Exception e) {
                        // Catch any other unexpected errors
                        System.err.println("Unexpected OCR error: " + e.getMessage());
                        textToStore.append("[OCR error - file saved without text extraction]\n");
                    }
                }
            }
    
            // Handle plain text
            if (!body.isBlank()) {
                textToStore.append(body);
                String fileName = from.replace(":", "_") + "_" + System.currentTimeMillis() + ".txt";
                fileStorageService.saveText(body, fileName);
            }
    
            if (!textToStore.toString().isBlank()) {
                // generate embedding + save
                List<Double> embeddingList = embeddingService.generateEmbedding(textToStore.toString());
                Double[] embeddingArray = embeddingList.toArray(new Double[0]);
                knowledgeBaseRepository.save(new KnowledgeEntry(from, textToStore.toString(), embeddingArray));
            }
    
            return "✅ Stored successfully! Type 'end' to finish or send more text/files.";
        } catch (IOException e) {
            e.printStackTrace();
            return "❌ Failed to save. Try again.";
        }
    }

    private String promptOptions() {
        return "Please choose an option:\n\n" +
               "1️⃣ Store in Knowledge Base\n" +
               "2️⃣ Chat with AI";
    }

    /**
     * Basic Twilio WA sender - only works with valid WhatsApp phone numbers
     */
    public void sendMessage(String to, String text) {
        try {
            Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioConfig.getFromNumber()),
                    text
            ).create();
        } catch (Exception e) {
            // Log but don't crash - this allows frontend calls to work
            System.err.println("Failed to send Twilio message to " + to + ": " + e.getMessage());
        }
    }

    /**
     * Simpler option menu (always works)
     */
    public void sendTextOptions(String to) {
        sendMessage(to, promptOptions());
    }
}