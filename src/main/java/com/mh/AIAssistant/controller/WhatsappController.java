package com.mh.AIAssistant.controller; 

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mh.AIAssistant.enums.UserMode;
import com.mh.AIAssistant.model.KnowledgeEntry;
import com.mh.AIAssistant.repository.KnowledgeBaseRepository;
import com.mh.AIAssistant.service.DeepSeekAIService;
import com.mh.AIAssistant.service.DeepSeekEmbeddingService;
import com.mh.AIAssistant.service.FileStorageService;
import com.mh.AIAssistant.service.OcrService;
import com.mh.AIAssistant.service.OpenAIEmbeddingService;

import jakarta.annotation.Resource;
import net.sourceforge.tess4j.TesseractException;

@RestController
@RequestMapping("/whatsapp")
public class WhatsappController {

    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private OcrService ocrService;

    @Resource
    private OpenAIEmbeddingService embeddingService;

    @Resource
    private DeepSeekAIService deepSeekAIService;

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    private Map<String, UserMode> userSessions = new HashMap<>();

    @GetMapping("/hello")
    public String hello() {
        return "Hello AI!";
    }

    @PostMapping("/incoming")
    public String receiveMessage(@RequestParam("From") String from,
                                 @RequestParam("Body") String body) {
        System.out.println("Message from " + from + ": " + body);

        // Simple reply
        String response = "You said: " + body;

        // Twilio expects TwiML (XML) response
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response><Message>" + response + "</Message></Response>";
    }

    @PostMapping("/incoming_2")
    public String receiveMessage2(@RequestParam Map<String,String> params) {
        String from = params.get("From");                 // e.g. "whatsapp:+60123..."
        String body = params.getOrDefault("Body", "");
        int numMedia = Integer.parseInt(params.getOrDefault("NumMedia", "0"));

        System.out.println("Incoming From=" + from + " Body=" + body + " NumMedia=" + numMedia);

        if (numMedia > 0) {
            // media message
            String mediaUrl = params.get("MediaUrl0");
            try {
                String fileName = from.replace(":", "_") + "_" + System.currentTimeMillis();
                String savedPath = fileStorageService.saveFile(mediaUrl, fileName);
                System.out.println("File saved: " + savedPath);
                return twiml("I saved your file locally in doc/!");
            } catch (Exception e) {
                e.printStackTrace();
                return twiml("Sorry, I couldn't save the file.");
            }
        } else {
            // text message
            // TODO: prompt user to choose store vs chat, or process immediately
            return twiml("You said: " + body);
        }
    }

    @PostMapping("/incoming_manual")
    public String receiveMessage(@RequestParam Map<String,String> params) {
        String from = params.get("From");
        String body = params.getOrDefault("Body", "").trim();
        int numMedia = Integer.parseInt(params.getOrDefault("NumMedia", "0"));

        // Initialize session if missing
        userSessions.putIfAbsent(from, UserMode.NONE);
        UserMode currentMode = userSessions.get(from);

        String reply;

        // Check if user wants to end session
        if ("end".equalsIgnoreCase(body)) {
            userSessions.put(from, UserMode.NONE);
            reply = "✅ Session ended. You can start again anytime.\n\n" + promptOptions();
            // return twiml(reply);
            return reply;
        }

        switch(currentMode) {
            case NONE:
                // Expect user to choose 1 or 2
                if ("1".equals(body)) {
                    userSessions.put(from, UserMode.STORE);
                    reply = "📥 Store mode activated. Send me the text or document you want to store. Type 'end' to finish.";
                } else if ("2".equals(body)) {
                    userSessions.put(from, UserMode.CHAT);
                    reply = "🤖 Chat mode activated. Ask me any question. Type 'end' to finish.";
                } else {
                    reply = "Please choose an option:\n" + promptOptions();
                }
                break;

            case STORE:
                // Handle STORE mode
                try {
                    String textToStore = "";

                    if (numMedia > 0) {
                        for (int i = 0; i < numMedia; i++) {
                            String mediaUrl = params.get("MediaUrl" + i);
                            String fileName = from.replace(":", "_") + "_" + System.currentTimeMillis() + "_" + i;
                            String savedPath = fileStorageService.saveFile(mediaUrl, fileName);
                            System.out.println("File saved: " + savedPath);

                            // OCR extractionso 
                            File file = new File(savedPath);

                            // Only process if it's an image/pdf
                            String extractedText = "";
                            try {
                                extractedText = ocrService.extractText(file);
                            } catch (TesseractException e) {
                                e.printStackTrace();
                                extractedText = "[OCR failed]";
                            }

                            textToStore += extractedText + "\n";

                        }
                    }

                    if (!body.isBlank()) {
                        // Include plain text
                        textToStore += body;
                        String fileName = from.replace(":", "_") + "_" + System.currentTimeMillis() + ".txt";
                        fileStorageService.saveText(body, fileName);
                    }

                    if (!textToStore.isBlank()) {
                        // Generate embedding
                        List<Double> embeddingList = embeddingService.generateEmbedding(textToStore);

                        // Convert List<Double> to Double[]
                        Double[] embeddingArray = embeddingList.toArray(new Double[0]);

                        // Save to PostgreSQL
                        knowledgeBaseRepository.save(new KnowledgeEntry(from, textToStore, embeddingArray));
                    }


                    reply = "✅ Stored successfully! Type 'end' to finish or send more text/files.";

                } catch (IOException e) {
                    e.printStackTrace();
                    reply = "❌ Failed to save. Try again.";
                }
                break;

            case CHAT:
                // Fetch relevant knowledge entries from DB (optional)
                List<KnowledgeEntry> entries = knowledgeBaseRepository.findByUserId(from);
                List<String> contextTexts = entries.stream()
                                                .map(KnowledgeEntry::getContent)
                                                .toList();

                String aiReply = deepSeekAIService.chatWithKnowledge(body, contextTexts);
                reply = "🤖 AI says: " + aiReply + "\n\n(Type 'end' to finish chat)";
                break;


            default:
                reply = promptOptions();
                break;
        }

        // return twiml(reply);
        return reply;
    }

    private String promptOptions() {
        return "1️⃣ Store in Knowledge Base\n2️⃣ Chat with AI";
    }

    private String twiml(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<Response><Message>" + message + "</Message></Response>";
    }

}
