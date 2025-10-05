package com.mh.AIAssistant.service;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.mh.AIAssistant.configuration.TwilioConfig;


@Service
public class FileStorageService {

    private final TwilioConfig twilioConfig;
    
    @Value("${file.storage.path}")
    private String folderPath;

    public FileStorageService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    public String saveText(String text, String userFrom) throws IOException {
        Path folder = Paths.get(folderPath);
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        // Make filename safe
        String safeUser = userFrom.replace(":", "_").replace("+", "_");
        String fileName = safeUser + "_" + System.currentTimeMillis() + ".txt";

        Path filePath = folder.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE)) {
            writer.write(text);
        }

        return filePath.toString();
    }

    public String saveFile(String mediaUrl, String fileName) throws IOException {
        Path folder = Paths.get(folderPath);
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        String accountSid = twilioConfig.getAccountSid();
        String authToken = twilioConfig.getAuthToken();

        URL url = new URL(mediaUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        // Twilio requires authentication (Account SID + Auth Token)
        String userPass = accountSid + ":" + authToken;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userPass.getBytes());
        conn.setRequestProperty("Authorization", basicAuth);

        // Download the file
        try (InputStream in = conn.getInputStream()) {
            Path filePath = folder.resolve(fileName);
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        }
    }

    public String saveMultipartFile(MultipartFile file, String fileName) throws IOException {
        Path folder = Paths.get(folderPath);
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path filePath = folder.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath.toString();
    }
}