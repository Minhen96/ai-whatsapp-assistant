package com.mh.AIAssistant.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

@Service
public class OcrService {
    
    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);
    private final Tesseract tesseract;
    
    // Image formats that need OCR
    private static final List<String> IMAGE_FORMATS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp"
    );
    
    // Plain text formats
    private static final List<String> TEXT_FORMATS = Arrays.asList(
        "txt", "md", "csv", "json", "xml", "html", "css", "js", "java", "py", 
        "log", "yml", "yaml", "properties", "ini", "conf", "sh", "bat"
    );
    
    // Document formats
    private static final List<String> DOCUMENT_FORMATS = Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );
    
    public OcrService() {
        this.tesseract = new Tesseract();
        tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(1);
    }
    
    /**
     * Main entry point - intelligently extracts text based on file type
     */
    public String extractText(File file) throws TesseractException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        
        String filename = file.getName();
        String extension = getFileExtension(filename).toLowerCase();
        
        logger.info("Extracting text from file: {} (type: {})", filename, extension);
        
        try {
            // Plain text files
            if (TEXT_FORMATS.contains(extension)) {
                return readTextFile(file);
            }
            
            // Image files - OCR
            if (IMAGE_FORMATS.contains(extension)) {
                return extractFromImage(file);
            }
            
            // Document files
            if (DOCUMENT_FORMATS.contains(extension)) {
                return extractFromDocument(file, extension);
            }
            
            throw new IllegalArgumentException(
                "Unsupported file format: " + extension + ". " +
                "Supported formats: " +
                "\n- Text: " + String.join(", ", TEXT_FORMATS) +
                "\n- Images: " + String.join(", ", IMAGE_FORMATS) +
                "\n- Documents: " + String.join(", ", DOCUMENT_FORMATS)
            );
            
        } catch (IOException e) {
            logger.error("IO error extracting text from file: {}", filename, e);
            throw new TesseractException("Failed to read file: " + filename, e);
        }
    }
    
    /**
     * Read plain text files
     */
    private String readTextFile(File file) throws IOException {
        try {
            return Files.readString(file.toPath()).trim();
        } catch (IOException e) {
            // Fallback: read as bytes with UTF-8
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes, "UTF-8").trim();
        }
    }
    
    /**
     * Extract text from images using OCR
     */
    private String extractFromImage(File file) throws TesseractException {
        logger.info("Performing OCR on image: {}", file.getName());
        String result = tesseract.doOCR(file);
        return result != null ? result.trim() : "";
    }
    
    /**
     * Route to appropriate document processor
     */
    private String extractFromDocument(File file, String extension) throws IOException, TesseractException {
        switch (extension) {
            case "pdf":
                return extractFromPDF(file);
            case "docx":
                return extractFromDOCX(file);
            case "doc":
                return extractFromDOC(file);
            case "xlsx":
                return extractFromXLSX(file);
            case "xls":
                return extractFromXLS(file);
            case "pptx":
                return extractFromPPTX(file);
            case "ppt":
                return extractFromPPT(file);
            default:
                throw new IllegalArgumentException("Document format not implemented: " + extension);
        }
    }
    
    /**
     * Extract text from PDF
     */
    private String extractFromPDF(File file) throws IOException {
        logger.info("Extracting text from PDF: {}", file.getName());
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text != null ? text.trim() : "";
        }
    }
    
    /**
     * Extract text from DOCX (Word 2007+)
     */
    private String extractFromDOCX(File file) throws IOException {
        logger.info("Extracting text from DOCX: {}", file.getName());
        StringBuilder sb = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                sb.append(paragraph.getText()).append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Extract text from DOC (Word 97-2003)
     */
    private String extractFromDOC(File file) throws IOException {
        logger.info("Extracting text from DOC: {}", file.getName());
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText().trim();
        }
    }
    
    /**
     * Extract text from XLSX (Excel 2007+)
     */
    private String extractFromXLSX(File file) throws IOException {
        logger.info("Extracting text from XLSX: {}", file.getName());
        StringBuilder sb = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(getCellValue(cell)).append("\t");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Extract text from XLS (Excel 97-2003)
     */
    private String extractFromXLS(File file) throws IOException {
        logger.info("Extracting text from XLS: {}", file.getName());
        StringBuilder sb = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(getCellValue(cell)).append("\t");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Extract text from PPTX (PowerPoint 2007+)
     */
    private String extractFromPPTX(File file) throws IOException {
        logger.info("Extracting text from PPTX: {}", file.getName());
        StringBuilder sb = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {
            
            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        sb.append(textShape.getText()).append("\n");
                    }
                }
                sb.append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Extract text from PPT (PowerPoint 97-2003)
     */
    private String extractFromPPT(File file) throws IOException {
        logger.info("Extracting text from PPT: {}", file.getName());
        StringBuilder sb = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             HSLFSlideShow ppt = new HSLFSlideShow(fis)) {
            
            for (HSLFSlide slide : ppt.getSlides()) {
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape) {
                        HSLFTextShape textShape = (HSLFTextShape) shape;
                        sb.append(textShape.getText()).append("\n");
                    }
                }
                sb.append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Helper: Get cell value as string
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return "";
        return filename.substring(lastDot + 1);
    }
    
    /**
     * Check if file type is supported
     */
    public boolean isSupported(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return TEXT_FORMATS.contains(extension) || 
               IMAGE_FORMATS.contains(extension) || 
               DOCUMENT_FORMATS.contains(extension);
    }
    
    /**
     * Get supported formats as string
     */
    public String getSupportedFormats() {
        return String.format(
            "Text files: %s\nImages: %s\nDocuments: %s",
            String.join(", ", TEXT_FORMATS),
            String.join(", ", IMAGE_FORMATS),
            String.join(", ", DOCUMENT_FORMATS)
        );
    }
}