package com.mh.AIAssistant.service;

import com.mh.AIAssistant.configuration.TwilioConfig;
import com.mh.AIAssistant.dto.DocumentInfo;
import com.mh.AIAssistant.enums.UserMode;
import com.mh.AIAssistant.model.KnowledgeEntry;
import com.mh.AIAssistant.repository.KnowledgeBaseRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WhatsappService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappService.class);

    private final TwilioConfig twilioConfig;
    private final FileStorageService fileStorageService;
    private final OcrService ocrService;
    private final OpenAIEmbeddingService embeddingService;
    private final DeepSeekAIService deepSeekAIService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseRepositoryCustom knowledgeBaseRepositoryCustom;
    private final DocumentService documentService;

    // simple in-memory session
    private final Map<String, UserMode> userSessions = new HashMap<>();

    // constructor injection
    public WhatsappService(
            TwilioConfig twilioConfig,
            FileStorageService fileStorageService,
            OcrService ocrService,
            OpenAIEmbeddingService embeddingService,
            DeepSeekAIService deepSeekAIService,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeBaseRepositoryCustom knowledgeBaseRepositoryCustom,
            DocumentService documentService
    ) {
        this.twilioConfig = twilioConfig;
        this.fileStorageService = fileStorageService;
        this.ocrService = ocrService;
        this.embeddingService = embeddingService;
        this.deepSeekAIService = deepSeekAIService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeBaseRepositoryCustom = knowledgeBaseRepositoryCustom;
        this.documentService = documentService;

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
    public void storeTextAndEmbed(String userId, String text) {
        storeTextAndEmbed(userId, text, null, null, null);
    }

    public void storeTextAndEmbed(String userId, String text, String filePath, 
                                String fileName, String fileType) {
        try {
            List<Double> embeddingList = embeddingService.getEmbedding(text);
            Double[] embeddingArray = embeddingList.toArray(new Double[0]);
            
            KnowledgeEntry entry = new KnowledgeEntry(
                userId, text, embeddingArray, filePath, fileName, fileType
            );
            knowledgeBaseRepository.save(entry);
            
            logger.info("Stored knowledge entry for user: {} with file: {}", userId, fileName);
        } catch (Exception e) {
            logger.error("Error storing text and embedding", e);
            throw new RuntimeException("Failed to store knowledge entry", e);
        }
    }

    /**
     * Reusable helper for web ChatController: produce AI reply using same knowledge retrieval as WA
     */
    public String chatReply(String userId, String userMessage) {
        try {
            // 1. Classify intent
            String intent = deepSeekAIService.classifyIntent(userId, userMessage);
            logger.info("Classified intent for user {}: {}", userId, intent);

            if ("RETRIEVE".equals(intent)) {
                // User wants to retrieve/find documents
                List<DocumentInfo> documents = 
                    documentService.findRelevantDocuments(userId, userMessage);
                
                if (documents.isEmpty()) {
                    return "I couldn't find any relevant documents in your knowledge base matching that query.";
                }

                // Let AI generate a natural response with document references
                List<String> contextTexts = documents.stream()
                    .map(doc -> {
                        String preview = doc.getContentPreview(300);
                        String fileInfo = doc.isHasFile() ? 
                            " [File: " + doc.getFileName() + "]" : "";
                        return preview + fileInfo;
                    })
                    .toList();

                String systemPrompt = String.format(
                    "The user asked: '%s'\n\n" +
                    "I found %d relevant document(s) in their knowledge base. " +
                    "Generate a natural, helpful response that:\n" +
                    "1. Acknowledges what they're looking for\n" +
                    "2. Mentions how many relevant documents were found\n" +
                    "3. Briefly describes what the documents contain\n" +
                    "4. If files are attached, mention they can click to download them\n\n" +
                    "Keep it conversational and helpful.",
                    userMessage,
                    documents.size()
                );

                return deepSeekAIService.chatWithKnowledge(userId, systemPrompt, contextTexts);

            } else {
                // CHAT mode - Answer questions using knowledge base
                List<Double> queryEmbedding = embeddingService.generateEmbedding(userMessage);
                if (queryEmbedding == null) {
                    logger.warn("Failed to generate embedding, fallback to general chat");
                    return deepSeekAIService.chat(userId, userMessage);
                }

                List<Object[]> rawResults = knowledgeBaseRepository.findSimilarEntriesRaw(
                        queryEmbedding.stream().map(String::valueOf).collect(Collectors.joining(",", "{", "}")),
                        userId, 5
                );

                List<String> contextTexts = rawResults.stream()
                    .filter(row -> {
                        Double sim = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
                        return sim >= 0.7;
                    })
                    .map(row -> (String) row[2])
                    .limit(3)
                    .toList();

                if (contextTexts.isEmpty()) {
                    logger.info("No relevant context found, using general chat");
                    return deepSeekAIService.chat(userId, userMessage);
                }

                return deepSeekAIService.chatWithKnowledge(userId, userMessage, contextTexts);
            }

        } catch (Exception e) {
            logger.error("Error in chatReply for user: {}", userId, e);
            return "I'm having trouble processing your request right now. Please try again.";
        }
    }

    public List<DocumentInfo> findRelevantDocuments(String userId, String query) {
        try {
            List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                logger.warn("Failed to generate embedding for query");
                return Collections.emptyList();
            }

            String embeddingStr = queryEmbedding.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "{", "}"));

            List<Object[]> rawResults = knowledgeBaseRepository.findSimilarEntriesRaw(
                    embeddingStr, userId, 20
            );

            // Use map to group documents by filename (fallback if filePath missing)
            Map<String, DocumentInfo> uniqueDocs = new HashMap<>();

            for (Object[] row : rawResults) {
                try {
                    Long id = ((Number) row[0]).longValue();
                    String content = (String) row[2];
                    Double similarity = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
                    String filePath = (String) row[6];
                    String fileName = (String) row[7];
                    String fileType = (String) row[8];

                    if (similarity < 0.3)
                        continue;

                    // Normalize key (use lowercase filename or filePath)
                    String key = (filePath != null && !filePath.isEmpty())
                            ? filePath.trim().toLowerCase()
                            : (fileName != null ? fileName.trim().toLowerCase() : "no_file");

                    // Skip duplicates
                    DocumentInfo existing = uniqueDocs.get(key);
                    if (existing == null) {
                        DocumentInfo doc = new DocumentInfo();
                        doc.setId(id);
                        doc.setContent(content);
                        doc.setSimilarity(similarity);
                        doc.setFilePath(filePath);
                        doc.setFileName(fileName);
                        doc.setFileType(fileType);
                        doc.setHasFile(filePath != null && !filePath.isEmpty());
                        uniqueDocs.put(key, doc);
                    } else {
                        // Merge content + take max similarity
                        existing.setContent(existing.getContent() + "\n" + content);
                        if (similarity > existing.getSimilarity()) {
                            existing.setSimilarity(similarity);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing row", e);
                }
            }

            List<DocumentInfo> result = new ArrayList<>(uniqueDocs.values());

            logger.info("✅ Found {} unique documents after deduplication (raw count: {})",
                    result.size(), rawResults.size());

            return result;

        } catch (Exception e) {
            logger.error("Error finding relevant documents", e);
            return Collections.emptyList();
        }
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