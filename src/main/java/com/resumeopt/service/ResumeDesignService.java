package com.resumeopt.service;

import com.resumeopt.model.ResumeDesign;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service for generating resumes with different ATS-friendly design templates.
 */
@Service
public class ResumeDesignService {
    
    @Value("${resume.design.default:MINIMAL}")
    private String defaultDesign;
    
    @Value("${resume.design.recommendation.enabled:true}")
    private boolean recommendationEnabled;
    
    /**
     * Generate PDF with specified design template
     */
    public byte[] generatePdfWithDesign(String content, ResumeDesign design) throws IOException {
        if (design == null) {
            design = ResumeDesign.fromString(defaultDesign);
        }
        
        String safeContent = sanitizeForPdf(content);
        
        switch (design) {
            case MINIMAL:
                return generateMinimalDesign(safeContent);
            case PROFESSIONAL:
                return generateProfessionalDesign(safeContent);
            case MODERN:
                return generateModernDesign(safeContent);
            case CREATIVE:
                return generateCreativeDesign(safeContent);
            case EXECUTIVE:
                return generateExecutiveDesign(safeContent);
            default:
                return generateMinimalDesign(safeContent);
        }
    }
    
    /**
     * Recommend best design based on resume content and job description
     */
    public ResumeDesign recommendDesign(String resumeText, String jobDescription) {
        if (!recommendationEnabled || resumeText == null || jobDescription == null) {
            return ResumeDesign.fromString(defaultDesign);
        }
        
        // Analyze content to determine best design
        String combinedText = (resumeText + " " + jobDescription).toLowerCase();
        
        // Check for creative/design roles
        if (containsKeywords(combinedText, Arrays.asList("design", "creative", "ui", "ux", "graphic", "art", "marketing", "branding"))) {
            return ResumeDesign.CREATIVE;
        }
        
        // Check for executive/senior roles
        if (containsKeywords(combinedText, Arrays.asList("executive", "director", "vp", "vice president", "senior", "lead", "manager", "head", "chief"))) {
            return ResumeDesign.EXECUTIVE;
        }
        
        // Check for modern/tech startup roles
        if (containsKeywords(combinedText, Arrays.asList("startup", "tech", "innovation", "agile", "scrum", "modern", "digital transformation"))) {
            return ResumeDesign.MODERN;
        }
        
        // Check for traditional corporate roles
        if (containsKeywords(combinedText, Arrays.asList("corporate", "business", "finance", "consulting", "enterprise", "traditional"))) {
            return ResumeDesign.PROFESSIONAL;
        }
        
        // Default to minimal for maximum ATS compatibility
        return ResumeDesign.MINIMAL;
    }
    
    /**
     * Get design preview information
     */
    public Map<String, Object> getDesignPreview(ResumeDesign design) {
        Map<String, Object> preview = new HashMap<>();
        preview.put("name", design.getDisplayName());
        preview.put("description", design.getDescription());
        preview.put("atsCompatibility", design.getAtsCompatibilityScore());
        preview.put("bestFor", design.getBestFor());
        preview.put("keyFeatures", getDesignFeatures(design));
        return preview;
    }
    
    private List<String> getDesignFeatures(ResumeDesign design) {
        switch (design) {
            case MINIMAL:
                return Arrays.asList("Single column layout", "Helvetica font", "Maximum whitespace", "100% ATS compatible");
            case PROFESSIONAL:
                return Arrays.asList("Traditional layout", "Times New Roman headers", "Clear sections", "Corporate-friendly");
            case MODERN:
                return Arrays.asList("Contemporary fonts", "Balanced layout", "Subtle styling", "Tech-friendly");
            case CREATIVE:
                return Arrays.asList("Unique layout", "Creative typography", "Visual hierarchy", "Design-focused");
            case EXECUTIVE:
                return Arrays.asList("Sophisticated layout", "Premium fonts", "Executive summary", "Leadership-focused");
            default:
                return Collections.emptyList();
        }
    }
    
    private boolean containsKeywords(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
    
    /**
     * MINIMAL Design - Single column, Helvetica font, minimal styling
     */
    private byte[] generateMinimalDesign(String content) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 60;
                float y = page.getMediaBox().getHeight() - margin;
                cs.setLeading(16f);
                
                // Parse content into sections
                Map<String, String> sections = parseSections(content);
                
                // Render sections
                for (Map.Entry<String, String> entry : sections.entrySet()) {
                    String sectionName = entry.getKey();
                    String sectionContent = entry.getValue();
                    
                    // Section header
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(sectionName.toUpperCase());
                    cs.endText();
                    y -= 20;
                    
                    // Section content
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.setLeading(14f);
                    cs.newLineAtOffset(margin, y);
                    
                    String[] lines = sectionContent.split("\n");
                    for (String line : lines) {
                        if (y < margin + 30) break;
                        String safeLine = sanitizeForPdf(line.trim());
                        if (!safeLine.isEmpty()) {
                            cs.showText(safeLine);
                            cs.newLine();
                            y -= 14;
                        }
                    }
                    cs.endText();
                    y -= 15; // Space between sections
                }
            }
            
            return saveDocument(doc);
        }
    }
    
    /**
     * PROFESSIONAL Design - Traditional layout, Times New Roman headers
     */
    private byte[] generateProfessionalDesign(String content) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                
                Map<String, String> sections = parseSections(content);
                
                for (Map.Entry<String, String> entry : sections.entrySet()) {
                    String sectionName = entry.getKey();
                    String sectionContent = entry.getValue();
                    
                    // Section header with underline effect
                    cs.beginText();
                    cs.setFont(PDType1Font.TIMES_BOLD, 13);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(sectionName.toUpperCase());
                    cs.endText();
                    
                    // Draw underline
                    cs.moveTo(margin, y - 2);
                    cs.lineTo(margin + 200, y - 2);
                    cs.stroke();
                    
                    y -= 18;
                    
                    // Section content
                    cs.beginText();
                    cs.setFont(PDType1Font.TIMES_ROMAN, 10);
                    cs.setLeading(13f);
                    cs.newLineAtOffset(margin, y);
                    
                    String[] lines = sectionContent.split("\n");
                    for (String line : lines) {
                        if (y < margin + 30) break;
                        String safeLine = sanitizeForPdf(line.trim());
                        if (!safeLine.isEmpty()) {
                            cs.showText(safeLine);
                            cs.newLine();
                            y -= 13;
                        }
                    }
                    cs.endText();
                    y -= 12;
                }
            }
            
            return saveDocument(doc);
        }
    }
    
    /**
     * MODERN Design - Contemporary fonts, balanced layout
     */
    private byte[] generateModernDesign(String content) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 55;
                float y = page.getMediaBox().getHeight() - margin;
                
                Map<String, String> sections = parseSections(content);
                
                for (Map.Entry<String, String> entry : sections.entrySet()) {
                    String sectionName = entry.getKey();
                    String sectionContent = entry.getValue();
                    
                    // Modern section header
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(sectionName.toUpperCase());
                    cs.endText();
                    y -= 18;
                    
                    // Section content with modern spacing
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.setLeading(15f);
                    cs.newLineAtOffset(margin + 5, y);
                    
                    String[] lines = sectionContent.split("\n");
                    for (String line : lines) {
                        if (y < margin + 30) break;
                        String safeLine = sanitizeForPdf(line.trim());
                        if (!safeLine.isEmpty()) {
                            cs.showText("• " + safeLine);
                            cs.newLine();
                            y -= 15;
                        }
                    }
                    cs.endText();
                    y -= 10;
                }
            }
            
            return saveDocument(doc);
        }
    }
    
    /**
     * CREATIVE Design - Unique layout, creative typography
     */
    private byte[] generateCreativeDesign(String content) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                
                Map<String, String> sections = parseSections(content);
                
                for (Map.Entry<String, String> entry : sections.entrySet()) {
                    String sectionName = entry.getKey();
                    String sectionContent = entry.getValue();
                    
                    // Creative section header with larger font
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(sectionName.toUpperCase());
                    cs.endText();
                    y -= 22;
                    
                    // Creative content layout
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.setLeading(16f);
                    cs.newLineAtOffset(margin + 10, y);
                    
                    String[] lines = sectionContent.split("\n");
                    for (String line : lines) {
                        if (y < margin + 30) break;
                        String safeLine = sanitizeForPdf(line.trim());
                        if (!safeLine.isEmpty()) {
                            cs.showText("→ " + safeLine);
                            cs.newLine();
                            y -= 16;
                        }
                    }
                    cs.endText();
                    y -= 12;
                }
            }
            
            return saveDocument(doc);
        }
    }
    
    /**
     * EXECUTIVE Design - Sophisticated layout, premium fonts
     */
    private byte[] generateExecutiveDesign(String content) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                
                Map<String, String> sections = parseSections(content);
                
                // Executive summary emphasis
                if (sections.containsKey("Summary") || sections.containsKey("Profile")) {
                    String summary = sections.getOrDefault("Summary", sections.getOrDefault("Profile", ""));
                    cs.beginText();
                    cs.setFont(PDType1Font.TIMES_BOLD, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("EXECUTIVE SUMMARY");
                    cs.endText();
                    y -= 20;
                    
                    cs.beginText();
                    cs.setFont(PDType1Font.TIMES_ROMAN, 10);
                    cs.setLeading(14f);
                    cs.newLineAtOffset(margin, y);
                    String[] summaryLines = summary.split("\n");
                    for (String line : summaryLines) {
                        if (y < margin + 30) break;
                        cs.showText(sanitizeForPdf(line.trim()));
                        cs.newLine();
                        y -= 14;
                    }
                    cs.endText();
                    y -= 20;
                }
                
                // Other sections
                for (Map.Entry<String, String> entry : sections.entrySet()) {
                    String sectionName = entry.getKey();
                    if (sectionName.equalsIgnoreCase("Summary") || sectionName.equalsIgnoreCase("Profile")) {
                        continue; // Already rendered
                    }
                    
                    String sectionContent = entry.getValue();
                    
                    // Executive section header
                    cs.beginText();
                    cs.setFont(PDType1Font.TIMES_BOLD, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(sectionName.toUpperCase());
                    cs.endText();
                    y -= 18;
                    
                    // Section content
                    cs.beginText();
                    cs.setFont(PDType1Font.TIMES_ROMAN, 10);
                    cs.setLeading(13f);
                    cs.newLineAtOffset(margin, y);
                    
                    String[] lines = sectionContent.split("\n");
                    for (String line : lines) {
                        if (y < margin + 30) break;
                        String safeLine = sanitizeForPdf(line.trim());
                        if (!safeLine.isEmpty()) {
                            cs.showText(safeLine);
                            cs.newLine();
                            y -= 13;
                        }
                    }
                    cs.endText();
                    y -= 12;
                }
            }
            
            return saveDocument(doc);
        }
    }
    
    /**
     * Parse content into sections
     */
    private Map<String, String> parseSections(String content) {
        Map<String, String> sections = new LinkedHashMap<>();
        
        // Common section patterns
        String[] sectionPatterns = {
            "Contact", "Personal Information", "Header",
            "Summary", "Profile", "Objective", "Executive Summary",
            "Education", "Academic", "Qualification",
            "Experience", "Work Experience", "Employment", "Professional Experience",
            "Projects", "Project Experience",
            "Technical Skills", "Skills", "Core Competencies",
            "Achievements", "Accomplishments", "Awards",
            "Certifications", "Certificates",
            "Skills & Keywords"
        };
        
        String[] lines = content.split("\n");
        String currentSection = "Header";
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            boolean isSectionHeader = false;
            String trimmedLine = line.trim();
            
            for (String pattern : sectionPatterns) {
                if (trimmedLine.matches("(?i).*" + pattern + ".*") && trimmedLine.length() < 50) {
                    if (currentContent.length() > 0) {
                        sections.put(currentSection, currentContent.toString().trim());
                    }
                    currentSection = pattern;
                    currentContent = new StringBuilder();
                    isSectionHeader = true;
                    break;
                }
            }
            
            if (!isSectionHeader && !trimmedLine.isEmpty()) {
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(trimmedLine);
            }
        }
        
        // Add last section
        if (currentContent.length() > 0) {
            sections.put(currentSection, currentContent.toString().trim());
        }
        
        return sections;
    }
    
    private String sanitizeForPdf(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u25E6' || c == '\u2022') {
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
    
    private byte[] saveDocument(PDDocument doc) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        doc.save(bos);
        doc.close();
        return bos.toByteArray();
    }
}

