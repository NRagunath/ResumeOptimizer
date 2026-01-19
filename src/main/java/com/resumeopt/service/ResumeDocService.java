package com.resumeopt.service;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeDocService {
    
    @Autowired(required = false)
    private AdvancedTemplatePreservationService templateService;

    public static class ChangeLog {
        public final List<String> entries = new ArrayList<>();
        public String toText(){
            return String.join("\n", entries);
        }
    }

    /**
     * Replace content in DOCX while preserving template and formatting.
     * Maps optimized text to original document structure.
     */
    public Result replaceContentWithOptimized(byte[] originalDocxBytes, String originalText, 
                                            String optimizedText, List<String> keywords) {
        // Use advanced template preservation if available
        if (templateService != null) {
            try {
                byte[] preserved = templateService.preserveDocxTemplate(
                    originalDocxBytes, originalText, optimizedText, keywords);
                ChangeLog log = new ChangeLog();
                log.entries.add("Preserved exact template formatting using advanced method");
                return new Result(preserved, log.toText());
            } catch (Exception e) {
                System.err.println("Advanced template preservation failed, falling back to basic method: " + e.getMessage());
            }
        }
        
        // Fallback to basic method
        try (ByteArrayInputStream in = new ByteArrayInputStream(originalDocxBytes);
             XWPFDocument doc = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            ChangeLog log = new ChangeLog();
            
            // Get all paragraphs and replace text while preserving formatting
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            String[] optimizedLines = optimizedText.split("\n");
            int optimizedIndex = 0;
            
            // Replace text in existing paragraphs, preserving formatting
            for (XWPFParagraph para : paragraphs) {
                if (optimizedIndex >= optimizedLines.length) break;
                
                List<XWPFRun> runs = para.getRuns();
                if (!runs.isEmpty()) {
                    // Keep first run's formatting, replace text
                    XWPFRun firstRun = runs.get(0);
                    String paraText = para.getText();
                    
                    // If paragraph has content, replace it with optimized content
                    if (paraText != null && !paraText.trim().isEmpty() && optimizedIndex < optimizedLines.length) {
                        // Clear existing runs except first
                        for (int i = runs.size() - 1; i > 0; i--) {
                            para.removeRun(i);
                        }
                        // Replace text in first run, preserving formatting
                        firstRun.setText(optimizedLines[optimizedIndex], 0);
                        optimizedIndex++;
                        log.entries.add("Replaced paragraph content while preserving formatting");
                    }
                }
            }
            
            // If there's remaining optimized content, add it with similar formatting
            while (optimizedIndex < optimizedLines.length) {
                XWPFParagraph newPara = doc.createParagraph();
                if (!paragraphs.isEmpty()) {
                    // Copy formatting from last paragraph
                    XWPFParagraph lastPara = paragraphs.get(paragraphs.size() - 1);
                    if (!lastPara.getRuns().isEmpty()) {
                        XWPFRun lastRun = lastPara.getRuns().get(0);
                        XWPFRun newRun = newPara.createRun();
                        newRun.setText(optimizedLines[optimizedIndex]);
                        newRun.setBold(lastRun.isBold());
                        newRun.setItalic(lastRun.isItalic());
                        // Font size - preserve if available (deprecated method but still functional)
                        try {
                            int fontSize = lastRun.getFontSize();
                            if (fontSize > 0) {
                                newRun.setFontSize(fontSize);
                            }
                        } catch (Exception e) {
                            // Ignore if font size cannot be retrieved
                        }
                        newRun.setColor(lastRun.getColor());
                    } else {
                        newPara.createRun().setText(optimizedLines[optimizedIndex]);
                    }
                } else {
                    newPara.createRun().setText(optimizedLines[optimizedIndex]);
                }
                optimizedIndex++;
            }
            
            doc.write(out);
            return new Result(out.toByteArray(), log.toText());
        } catch (Exception e) {
            // Fallback to append method
            return appendOptimizations(originalDocxBytes, optimizedText, keywords);
        }
    }
    
    /**
     * Preserve DOCX formatting while appending optimized sections and highlighted additions.
     * Adds a new section "Optimizations" with keywords and summary.
     */
    public Result appendOptimizations(byte[] originalDocxBytes, String optimizedSummary, List<String> keywords){
        try (ByteArrayInputStream in = new ByteArrayInputStream(originalDocxBytes);
             XWPFDocument doc = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()){

            ChangeLog log = new ChangeLog();

            // Section heading
            XWPFParagraph heading = doc.createParagraph();
            heading.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun hRun = heading.createRun();
            hRun.setText("Optimizations");
            hRun.setBold(true);
            hRun.setFontSize(12);
            log.entries.add("Added section: Optimizations");

            // Summary paragraph (highlighted)
            XWPFParagraph p = doc.createParagraph();
            XWPFRun run = p.createRun();
            run.setText(optimizedSummary);
            run.setColor("0070C0"); // blue highlight to indicate changes
            log.entries.add("Inserted optimized summary text");

            // Keywords line
            if (keywords != null && !keywords.isEmpty()){
                XWPFParagraph pk = doc.createParagraph();
                XWPFRun r = pk.createRun();
                r.setText("Keywords: " + String.join(", ", keywords));
                r.setItalic(true);
                r.setColor("7030A0"); // purple
                log.entries.add("Added keywords: " + String.join(", ", keywords));
            }

            doc.write(out);
            return new Result(out.toByteArray(), log.toText());
        } catch (Exception e){
            // Fallback: provide plain text DOCX with optimization summary
            try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()){
                XWPFParagraph p = doc.createParagraph();
                XWPFRun r = p.createRun();
                r.setText("Optimized Summary:\n" + optimizedSummary);
                if (keywords != null && !keywords.isEmpty()){
                    XWPFParagraph pk = doc.createParagraph();
                    XWPFRun rk = pk.createRun();
                    rk.setText("Keywords: " + String.join(", ", keywords));
                }
                doc.write(out);
                ChangeLog log = new ChangeLog();
                log.entries.add("Created minimal DOCX due to processing error");
                return new Result(out.toByteArray(), log.toText());
            } catch (Exception ex){
                return new Result("DOCX processing failed".getBytes(StandardCharsets.UTF_8), "Processing failed");
            }
        }
    }

    public static class Result {
        public final byte[] docxBytes;
        public final String changeLogText;
        public Result(byte[] docxBytes, String changeLogText){
            this.docxBytes = docxBytes;
            this.changeLogText = changeLogText;
        }
    }
}