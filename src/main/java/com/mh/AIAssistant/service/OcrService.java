package com.mh.AIAssistant.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class OcrService {
    
    private final Tesseract tesseract;
    
    public OcrService() {
        this.tesseract = new Tesseract();
        
        // Set the path to tessdata directory
        tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
        
        // Set language (default is English)
        tesseract.setLanguage("eng");
        
        // Optional: improve accuracy
        tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
        tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
    }
    
    public String extractText(File file) throws TesseractException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        
        try {
            String result = tesseract.doOCR(file);
            return result != null ? result.trim() : "";
        } catch (TesseractException e) {
            throw new TesseractException("OCR failed for file: " + file.getName(), e);
        }
    }
}