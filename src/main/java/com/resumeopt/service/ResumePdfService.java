package com.resumeopt.service;

import com.resumeopt.model.ResumeDesign;
import com.resumeopt.model.ResumeTemplateStyle;
import com.resumeopt.model.StructuredResumeView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ResumePdfService {
    
    @Autowired(required = false)
    private AdvancedTemplatePreservationService templateService;
    
    @Autowired(required = false)
    private ResumeDesignService designService;

    @Autowired(required = false)
    private TemplatePdfRenderer templatePdfRenderer;
    
    /**
     * Generate PDF from optimized text, preserving original template design if provided.
     * 
     * @param optimizedText The optimized resume text
     * @param originalPdfBytes Optional original PDF file bytes to preserve template
     * @param originalText Original resume text for structure parsing
     * @param design Optional resume design template to apply
     * @return PDF bytes
     */
    public byte[] generatePdf(String optimizedText, byte[] originalPdfBytes, String originalText, ResumeDesign design) throws IOException {
        // PRIORITY 1: If original PDF provided with original text, try to preserve design with new content
        if (originalPdfBytes != null && originalPdfBytes.length > 0 && originalText != null && templateService != null) {
            try {
                System.out.println("Attempting to preserve PDF design with optimized content...");
                return templateService.preservePdfTemplate(originalPdfBytes, originalText, optimizedText);
            } catch (Exception e) {
                System.err.println("PDF template preservation failed: " + e.getMessage());
                // Fall through to other methods
            }
        }
        
        // PRIORITY 2: If design is specified and design service is available, use design template
        if (design != null && designService != null) {
            try {
                return designService.generatePdfWithDesign(optimizedText, design);
            } catch (Exception e) {
                System.err.println("Design PDF generation failed, falling back: " + e.getMessage());
            }
        }
        
        // PRIORITY 3: Generate a well-formatted PDF from text
        return generateFormattedPdf(optimizedText);
    }
    
    /**
     * Generate PDF from optimized text, preserving original template if provided.
     * @param optimizedText The optimized resume text
     * @param originalPdfBytes Optional original PDF file bytes to preserve template
     * @param originalText Original resume text for structure parsing
     * @return PDF bytes
     */
    public byte[] generatePdf(String optimizedText, byte[] originalPdfBytes, String originalText) throws IOException {
        return generatePdf(optimizedText, originalPdfBytes, originalText, null);
    }
    
    /**
     * Generate PDF from optimized text, preserving original template if provided.
     * @param optimizedText The optimized resume text
     * @param originalPdfBytes Optional original PDF file bytes to preserve template
     * @return PDF bytes
     */
    public byte[] generatePdf(String optimizedText, byte[] originalPdfBytes) throws IOException {
        return generatePdf(optimizedText, originalPdfBytes, null);
    }
    
    /**
     * Generate PDF preserving the original template structure.
     * For PDFs, template preservation is complex, so we preserve page structure and dimensions.
     */
    private byte[] generatePdfFromTemplate(String optimizedText, byte[] originalPdfBytes) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(originalPdfBytes);
             PDDocument originalDoc = PDDocument.load(bis);
             PDDocument newDoc = new PDDocument()) {
            
            String safeText = sanitizeForPdf(optimizedText);
            
            // Preserve all pages from original, maintaining structure
            for (int i = 0; i < originalDoc.getNumberOfPages(); i++) {
                PDPage originalPage = originalDoc.getPage(i);
                PDRectangle pageSize = originalPage.getMediaBox();
                
                // Create new page with same dimensions
                PDPage newPage = new PDPage(pageSize);
                newDoc.addPage(newPage);
                
                try (PDPageContentStream cs = new PDPageContentStream(newDoc, newPage)) {
                    // Import original page content as form XObject to preserve visual layout
                    // This is a simplified approach - for full template preservation, 
                    // we would need to extract and map text positions
                    
                    // For now, we preserve page structure and write optimized content
                    // with formatting similar to original
                    float margin = 50;
                    float y = pageSize.getHeight() - margin;
                    cs.setLeading(14f);
                    
                    // Write optimized text preserving page layout
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 11);
                    cs.newLineAtOffset(margin, y);
                    
                    int charsPerLine = (int)((pageSize.getWidth() - 2 * margin) / 6); // Approximate
                    String[] lines = wrap(safeText, charsPerLine);
                    for (String line : lines) {
                        if (y < margin + 20) break; // Stop if we run out of space
                        cs.showText(line);
                        cs.newLine();
                        y -= 14;
                    }
                    cs.endText();
                }
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            newDoc.save(bos);
            return bos.toByteArray();
        }
    }
    
    public byte[] generatePdf(String optimizedText) throws IOException {
        return generateFormattedPdf(optimizedText);
    }

    /**
     * Generate a template-based PDF from a structured resume view.
     * This bypasses original template preservation and focuses on clean ATS layout.
     */
    public byte[] generateTemplatePdf(StructuredResumeView view, ResumeTemplateStyle style) throws IOException {
        if (templatePdfRenderer == null) {
            // Fallback: flatten structured view into text and use standard generator
            StringBuilder sb = new StringBuilder();
            if (view.getHeader() != null && view.getHeader().getFullName() != null) {
                sb.append(view.getHeader().getFullName()).append("\n");
            }
            if (view.getSummary() != null) {
                sb.append("\nSummary\n").append(view.getSummary()).append("\n");
            }
            return generateFormattedPdf(sb.toString());
        }
        return templatePdfRenderer.render(view, style);
    }
    
    /**
     * Generate a professionally formatted PDF with proper sections
     */
    private byte[] generateFormattedPdf(String optimizedText) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            
            String safeText = sanitizeForPdf(optimizedText);
            String[] lines = safeText.split("\n");
            
            float margin = 50;
            float rightMargin = page.getMediaBox().getWidth() - margin;
            float y = page.getMediaBox().getHeight() - margin;
            float lineHeight = 14f;
            
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setLeading(lineHeight);
                
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        y -= lineHeight / 2; // Half line for empty lines
                        continue;
                    }
                    
                    // Check if this is a section header
                    boolean isHeader = isSectionHeader(trimmedLine);
                    
                    // Check if we need a new page
                    if (y < margin + 50) {
                        cs.close();
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                    }
                    
                    cs.beginText();
                    if (isHeader) {
                        // Section headers - bold and slightly larger
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        y -= 8; // Extra space before headers
                    } else if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*") || trimmedLine.startsWith("•")) {
                        // Bullet points
                        cs.setFont(PDType1Font.HELVETICA, 10);
                    } else {
                        // Regular text
                        cs.setFont(PDType1Font.HELVETICA, 10);
                    }
                    
                    cs.newLineAtOffset(margin, y);
                    
                    // Wrap long lines
                    int maxChars = isHeader ? 70 : 85;
                    if (trimmedLine.length() > maxChars) {
                        String[] wrapped = wrapLine(trimmedLine, maxChars);
                        for (int i = 0; i < wrapped.length; i++) {
                            if (i > 0) {
                                cs.newLine();
                                y -= lineHeight;
                            }
                            cs.showText(wrapped[i]);
                        }
                    } else {
                        cs.showText(trimmedLine);
                    }
                    
                    cs.endText();
                    y -= lineHeight;
                    
                    if (isHeader) {
                        // Draw underline for headers
                        cs.moveTo(margin, y + 2);
                        cs.lineTo(margin + 150, y + 2);
                        cs.setLineWidth(0.5f);
                        cs.stroke();
                        y -= 4;
                    }
                }
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.save(bos);
            return bos.toByteArray();
        }
    }
    
    /**
     * Check if a line is a section header
     */
    private boolean isSectionHeader(String line) {
        String lower = line.toLowerCase().trim();
        String[] headers = {"education", "experience", "work experience", "skills", "technical skills",
            "projects", "achievements", "certifications", "summary", "objective", "profile",
            "contact", "references", "awards", "publications", "languages", "interests"};
        
        for (String header : headers) {
            if (lower.equals(header) || lower.startsWith(header + ":") || lower.startsWith(header + " ")) {
                return true;
            }
        }
        // Also check for all-caps short lines (common header format)
        return line.length() < 30 && line.equals(line.toUpperCase()) && line.matches(".*[A-Z].*");
    }
    
    /**
     * Wrap a single line into multiple lines
     */
    private String[] wrapLine(String text, int maxWidth) {
        java.util.List<String> result = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        
        for (String word : words) {
            if (current.length() + word.length() + 1 > maxWidth) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
            }
            if (current.length() > 0) current.append(" ");
            current.append(word);
        }
        if (current.length() > 0) result.add(current.toString());
        
        return result.toArray(new String[0]);
    }

    /**
     * Replace characters that Helvetica WinAnsiEncoding cannot display with safe equivalents.
     * Currently we explicitly handle the U+25E6 "WHITE BULLET" and fall back to a space for
     * any other non-ASCII characters.
     */
    private String sanitizeForPdf(String text) {
        if (text == null) return "";

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Map specific problematic characters to simple ASCII equivalents
            if (c == '\u25E6') {           // WHITE BULLET
                sb.append('-');
            } else if (c >= 32 && c <= 126) {
                // Printable ASCII range which WinAnsi can handle
                sb.append(c);
            } else if (Character.isWhitespace(c)) {
                // Preserve whitespace characters
                sb.append(' ');
            } else {
                // Fallback for any other unsupported character
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private String[] wrap(String text, int width) {
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String w : words) {
            if (sb.length() + w.length() + 1 > width) {
                lines.add(sb.toString());
                sb.setLength(0);
            }
            if (sb.length() > 0) sb.append(' ');
            sb.append(w);
        }
        if (sb.length() > 0) lines.add(sb.toString());
        return lines.toArray(new String[0]);
    }
}