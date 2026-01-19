package com.resumeopt.service;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Advanced service for preserving exact resume template formatting
 * while replacing content with optimized text.
 */
@Service
public class AdvancedTemplatePreservationService {
    
    /**
     * Preserves DOCX template with exact formatting by intelligently mapping
     * optimized content to original structure.
     */
    public byte[] preserveDocxTemplate(byte[] originalDocxBytes, String originalText, 
                                       String optimizedText, List<String> injectedKeywords) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(originalDocxBytes);
             XWPFDocument doc = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Parse resume structure from original text
            ResumeStructure structure = parseResumeStructure(originalText);
            
            // Map optimized text to structure
            Map<String, String> optimizedSections = mapOptimizedToStructure(optimizedText, structure);
            
            // Replace content while preserving exact formatting
            replaceContentPreservingFormat(doc, structure, optimizedSections, injectedKeywords);
            
            doc.write(out);
            return out.toByteArray();
        }
    }
    
    /**
     * Preserves PDF template by extracting formatting and applying optimized content
     */
    public byte[] preservePdfTemplate(byte[] originalPdfBytes, String originalText,
                                    String optimizedText) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(originalPdfBytes);
             PDDocument originalDoc = PDDocument.load(bis);
             PDDocument newDoc = new PDDocument()) {
            
            // Extract formatting information from original
            List<PageFormat> pageFormats = extractPdfFormatting(originalDoc);
            
            // Parse resume structure
            ResumeStructure structure = parseResumeStructure(originalText);
            Map<String, String> optimizedSections = mapOptimizedToStructure(optimizedText, structure);
            
            // Recreate PDF with preserved formatting
            for (int i = 0; i < originalDoc.getNumberOfPages() && i < pageFormats.size(); i++) {
                PageFormat format = pageFormats.get(i);
                PDPage newPage = new PDPage(format.pageSize);
                newDoc.addPage(newPage);
                
                renderPdfWithFormatting(newDoc, newPage, optimizedSections, format);
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            newDoc.save(bos);
            return bos.toByteArray();
        }
    }
    
    /**
     * Parses resume structure to identify sections and their content
     */
    private ResumeStructure parseResumeStructure(String text) {
        ResumeStructure structure = new ResumeStructure();
        
        // Common resume section patterns
        Pattern[] sectionPatterns = {
            Pattern.compile("(?i)^\\s*(contact|personal information|contact information)"),
            Pattern.compile("(?i)^\\s*(education|academic|qualification)"),
            Pattern.compile("(?i)^\\s*(experience|work experience|employment|professional experience)"),
            Pattern.compile("(?i)^\\s*(projects?|project experience)"),
            Pattern.compile("(?i)^\\s*(technical skills?|skills?|core competencies)"),
            Pattern.compile("(?i)^\\s*(achievements?|accomplishments?|awards)"),
            Pattern.compile("(?i)^\\s*(certifications?|certificates?)"),
            Pattern.compile("(?i)^\\s*(summary|objective|profile)")
        };
        
        String[] lines = text.split("\n");
        String currentSection = "Header";
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            boolean isSectionHeader = false;
            for (Pattern pattern : sectionPatterns) {
                if (pattern.matcher(line).find()) {
                    if (currentContent.length() > 0) {
                        structure.sections.put(currentSection, currentContent.toString().trim());
                    }
                    currentSection = line.trim();
                    currentContent = new StringBuilder();
                    isSectionHeader = true;
                    break;
                }
            }
            
            if (!isSectionHeader) {
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(line);
            }
        }
        
        // Add last section
        if (currentContent.length() > 0) {
            structure.sections.put(currentSection, currentContent.toString().trim());
        }
        
        return structure;
    }
    
    /**
     * Maps optimized text to resume structure sections
     */
    private Map<String, String> mapOptimizedToStructure(String optimizedText, ResumeStructure structure) {
        Map<String, String> mapped = new LinkedHashMap<>();
        ResumeStructure optimizedStructure = parseResumeStructure(optimizedText);
        
        // Map each section from optimized to original structure
        for (String originalSection : structure.sections.keySet()) {
            // Find matching section in optimized text
            String matchedSection = findMatchingSection(originalSection, optimizedStructure);
            if (matchedSection != null) {
                mapped.put(originalSection, optimizedStructure.sections.get(matchedSection));
            } else {
                // Keep original if no match found
                mapped.put(originalSection, structure.sections.get(originalSection));
            }
        }
        
        // Add new sections from optimized text that don't exist in original
        for (String optSection : optimizedStructure.sections.keySet()) {
            if (!mapped.containsKey(optSection)) {
                mapped.put(optSection, optimizedStructure.sections.get(optSection));
            }
        }
        
        return mapped;
    }
    
    private String findMatchingSection(String originalSection, ResumeStructure optimized) {
        String lowerOriginal = originalSection.toLowerCase();
        
        // Direct match
        for (String optSection : optimized.sections.keySet()) {
            if (optSection.toLowerCase().equals(lowerOriginal)) {
                return optSection;
            }
        }
        
        // Fuzzy match
        for (String optSection : optimized.sections.keySet()) {
            String lowerOpt = optSection.toLowerCase();
            if (lowerOpt.contains(lowerOriginal) || lowerOriginal.contains(lowerOpt)) {
                return optSection;
            }
        }
        
        return null;
    }
    
    /**
     * Replaces content in DOCX while preserving exact formatting
     */
    private void replaceContentPreservingFormat(XWPFDocument doc, ResumeStructure structure,
                                               Map<String, String> optimizedSections,
                                               List<String> injectedKeywords) {
        List<XWPFParagraph> paragraphs = doc.getParagraphs();
        
        // Process paragraphs
        for (XWPFParagraph para : paragraphs) {
            String paraText = para.getText();
            
            // Check if this paragraph belongs to a section
            for (Map.Entry<String, String> entry : optimizedSections.entrySet()) {
                String sectionName = entry.getKey();
                String sectionContent = entry.getValue();
                
                // If paragraph text matches section content or is part of it
                if (paraText != null && !paraText.trim().isEmpty()) {
                    // Check if this is a section header
                    if (isSectionHeader(paraText, sectionName)) {
                        // Preserve header formatting, update text if needed
                        updateParagraphText(para, sectionName, true);
                    } else if (isPartOfSection(paraText, sectionContent)) {
                        // Replace content while preserving formatting
                        String replacement = extractReplacementText(paraText, sectionContent);
                        updateParagraphText(para, replacement, false);
                    }
                }
            }
        }
        
        // Add keywords section if not present and keywords exist
        if (injectedKeywords != null && !injectedKeywords.isEmpty()) {
            addKeywordsSection(doc, injectedKeywords);
        }
    }
    
    private boolean isSectionHeader(String text, String sectionName) {
        String lowerText = text.toLowerCase().trim();
        String lowerSection = sectionName.toLowerCase();
        return lowerText.equals(lowerSection) || lowerText.startsWith(lowerSection);
    }
    
    private boolean isPartOfSection(String paraText, String sectionContent) {
        if (sectionContent == null || paraText == null) return false;
        String lowerPara = paraText.toLowerCase();
        String lowerSection = sectionContent.toLowerCase();
        return lowerSection.contains(lowerPara) || lowerPara.length() > 10 && lowerSection.contains(lowerPara.substring(0, Math.min(20, lowerPara.length())));
    }
    
    private String extractReplacementText(String originalPara, String sectionContent) {
        // Try to find matching content in section
        String[] sectionLines = sectionContent.split("\n");
        for (String line : sectionLines) {
            if (line.toLowerCase().contains(originalPara.toLowerCase().substring(0, Math.min(10, originalPara.length())))) {
                return line;
            }
        }
        return originalPara; // Keep original if no match
    }
    
    private void updateParagraphText(XWPFParagraph para, String newText, boolean isHeader) {
        List<XWPFRun> runs = para.getRuns();
        
        if (runs.isEmpty()) {
            XWPFRun run = para.createRun();
            run.setText(newText);
            if (isHeader) {
                run.setBold(true);
                run.setFontSize(14);
            }
        } else {
            // Preserve first run's formatting
            XWPFRun firstRun = runs.get(0);
            
            // Remove other runs
            for (int i = runs.size() - 1; i > 0; i--) {
                para.removeRun(i);
            }
            
            // Update text preserving formatting
            firstRun.setText(newText, 0);
        }
    }
    
    private void addKeywordsSection(XWPFDocument doc, List<String> keywords) {
        // Check if keywords section already exists
        for (XWPFParagraph para : doc.getParagraphs()) {
            String text = para.getText();
            if (text != null && text.toLowerCase().contains("keywords") && text.toLowerCase().contains("ats")) {
                return; // Section already exists
            }
        }
        
        // Add new section
        XWPFParagraph heading = doc.createParagraph();
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Skills & Keywords (ATS-optimized)");
        headingRun.setBold(true);
        headingRun.setFontSize(12);
        
        XWPFParagraph content = doc.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText(String.join(", ", keywords));
        contentRun.setFontSize(10);
    }
    
    private List<PageFormat> extractPdfFormatting(PDDocument doc) throws IOException {
        List<PageFormat> formats = new ArrayList<>();
        
        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            PDPage page = doc.getPage(i);
            PageFormat format = new PageFormat();
            format.pageSize = page.getMediaBox();
            format.margin = 50; // Default, could be extracted
            formats.add(format);
        }
        
        return formats;
    }
    
    private void renderPdfWithFormatting(PDDocument doc, PDPage page, 
                                        Map<String, String> optimizedSections,
                                        PageFormat format) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float pageWidth = format.pageSize.getWidth();
            float y = format.pageSize.getHeight() - format.margin;
            float margin = format.margin;
            float contentWidth = pageWidth - (2 * margin);
            float lineHeight = 13f;
            
            // Draw header background (like the original resume)
            cs.setNonStrokingColor(0.96f, 0.87f, 0.70f); // Light orange/tan color
            cs.addRect(0, format.pageSize.getHeight() - 80, pageWidth, 80);
            cs.fill();
            cs.setNonStrokingColor(0, 0, 0); // Back to black
            
            boolean isFirstSection = true;
            
            for (Map.Entry<String, String> entry : optimizedSections.entrySet()) {
                String sectionName = entry.getKey();
                String sectionContent = entry.getValue();
                
                if (sectionContent == null || sectionContent.trim().isEmpty()) continue;
                
                // Check if we need a new page
                if (y < format.margin + 100) {
                    cs.close();
                    PDPage newPage = new PDPage(format.pageSize);
                    doc.addPage(newPage);
                    y = format.pageSize.getHeight() - format.margin;
                }
                
                // Handle Header section (name, contact) differently
                if (sectionName.equalsIgnoreCase("Header") || isFirstSection) {
                    isFirstSection = false;
                    y = format.pageSize.getHeight() - 50;
                    
                    // Name (first line, large and bold)
                    String[] headerLines = sectionContent.split("\n");
                    if (headerLines.length > 0) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(sanitizeForPdf(headerLines[0].trim()));
                        cs.endText();
                        y -= 25;
                        
                        // Contact info (smaller)
                        if (headerLines.length > 1) {
                            cs.beginText();
                            cs.setFont(PDType1Font.HELVETICA, 10);
                            cs.newLineAtOffset(margin, y);
                            for (int i = 1; i < headerLines.length; i++) {
                                String line = headerLines[i].trim();
                                if (!line.isEmpty()) {
                                    cs.showText(sanitizeForPdf(line) + "  ");
                                }
                            }
                            cs.endText();
                        }
                    }
                    y -= 30;
                    continue;
                }
                
                // Section header with underline
                y -= 8; // Space before section
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                String headerText = sectionName.toUpperCase();
                cs.showText(sanitizeForPdf(headerText));
                cs.endText();
                
                // Draw underline
                cs.setStrokingColor(0.8f, 0.6f, 0.2f); // Orange underline
                cs.setLineWidth(1.5f);
                cs.moveTo(margin, y - 3);
                cs.lineTo(margin + 120, y - 3);
                cs.stroke();
                cs.setStrokingColor(0, 0, 0);
                
                y -= 18;
                
                // Section content
                String[] lines = sectionContent.split("\n");
                cs.setFont(PDType1Font.HELVETICA, 10);
                
                for (String line : lines) {
                    if (y < format.margin + 20) break;
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        y -= lineHeight / 2;
                        continue;
                    }
                    
                    // Check if this is a sub-header (company name, degree, etc.)
                    boolean isSubHeader = trimmedLine.matches("^[A-Z].*\\d{4}.*") || 
                                         trimmedLine.contains(" - ") ||
                                         (trimmedLine.length() < 60 && !trimmedLine.startsWith("-") && !trimmedLine.startsWith("•"));
                    
                    cs.beginText();
                    if (isSubHeader && !trimmedLine.startsWith("-") && !trimmedLine.startsWith("•")) {
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                    } else {
                        cs.setFont(PDType1Font.HELVETICA, 10);
                    }
                    cs.newLineAtOffset(margin + 5, y);
                    
                    // Wrap long lines
                    String safeLine = sanitizeForPdf(trimmedLine);
                    if (safeLine.length() > 95) {
                        String[] wrapped = wrapText(safeLine, 95);
                        for (int i = 0; i < wrapped.length; i++) {
                            if (i > 0) {
                                cs.endText();
                                y -= lineHeight;
                                cs.beginText();
                                cs.setFont(PDType1Font.HELVETICA, 10);
                                cs.newLineAtOffset(margin + 10, y);
                            }
                            cs.showText(wrapped[i]);
                        }
                    } else {
                        cs.showText(safeLine);
                    }
                    cs.endText();
                    y -= lineHeight;
                }
                y -= 5; // Space after section
            }
            
            // Footer
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
            cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
            cs.newLineAtOffset(pageWidth / 2 - 40, 30);
            cs.showText("Page 1 of 1");
            cs.endText();
        }
    }
    
    private String[] wrapText(String text, int maxChars) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        
        for (String word : words) {
            if (current.length() + word.length() + 1 > maxChars) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
            }
            if (current.length() > 0) current.append(" ");
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        
        return lines.toArray(new String[0]);
    }
    
    private String sanitizeForPdf(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u25E6' || c == '\u2022') { // Bullet points
                sb.append("•");
            } else if (c >= 32 && c <= 126) {
                sb.append(c);
            } else if (Character.isWhitespace(c)) {
                sb.append(' ');
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
    
    // Helper classes
    private static class ResumeStructure {
        Map<String, String> sections = new LinkedHashMap<>();
    }
    
    private static class PageFormat {
        PDRectangle pageSize;
        float margin;
    }
}

