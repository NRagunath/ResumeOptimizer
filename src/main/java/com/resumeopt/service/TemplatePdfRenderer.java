package com.resumeopt.service;

import com.resumeopt.model.ResumeTemplateStyle;
import com.resumeopt.model.StructuredResumeView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Renders a StructuredResumeView into a clean, ATS-friendly single-column PDF.
 */
@Service
public class TemplatePdfRenderer {

    public byte[] render(StructuredResumeView view, ResumeTemplateStyle style) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;
            float lineHeight = 13f;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setLeading(lineHeight);

                // Header
                y = writeHeader(cs, page, view, margin, y, style);

                // Summary
                if (view.getSummary() != null && !view.getSummary().isBlank()) {
                    y = writeSectionHeader(cs, page, margin, y, "Summary");
                    y = writeParagraph(cs, page, margin, y, view.getSummary(), lineHeight, 90);
                }

                // Skills
                if (view.getSkills() != null && !view.getSkills().isEmpty()) {
                    y = writeSectionHeader(cs, page, margin, y, "Skills");
                    for (var group : view.getSkills()) {
                        String title = group.getGroupName() != null ? group.getGroupName() + ": " : "";
                        String skillsLine = String.join(", ", group.getSkills());
                        y = writeParagraph(cs, page, margin, y, title + skillsLine, lineHeight, 95);
                    }
                }

                // Experience
                if (view.getExperience() != null && !view.getExperience().isEmpty()) {
                    y = writeSectionHeader(cs, page, margin, y, "Experience");
                    for (var item : view.getExperience()) {
                        y = ensureSpace(doc, cs, page, margin, y, lineHeight * 4);
                        y = writeBoldLine(cs, page, margin, y,
                                safe(item.getTitle()) + (item.getCompany() != null ? " - " + safe(item.getCompany()) : ""));
                        for (String b : item.getBullets()) {
                            y = writeBullet(cs, page, margin + 10, y, b, lineHeight, 90);
                        }
                    }
                }

                // Projects
                if (view.getProjects() != null && !view.getProjects().isEmpty()) {
                    y = writeSectionHeader(cs, page, margin, y, "Projects");
                    for (var item : view.getProjects()) {
                        y = ensureSpace(doc, cs, page, margin, y, lineHeight * 4);
                        y = writeBoldLine(cs, page, margin, y, safe(item.getName()));
                        for (String b : item.getBullets()) {
                            y = writeBullet(cs, page, margin + 10, y, b, lineHeight, 90);
                        }
                    }
                }

                // Education
                if (view.getEducation() != null && !view.getEducation().isEmpty()) {
                    y = writeSectionHeader(cs, page, margin, y, "Education");
                    for (var item : view.getEducation()) {
                        y = ensureSpace(doc, cs, page, margin, y, lineHeight * 3);
                        y = writeBoldLine(cs, page, margin, y, safe(item.getInstitution()));
                        String line = "";
                        if (item.getDegree() != null) line += safe(item.getDegree());
                        if (item.getFieldOfStudy() != null) line += " - " + safe(item.getFieldOfStudy());
                        if (!line.isBlank()) {
                            y = writeParagraph(cs, page, margin, y, line, lineHeight, 95);
                        }
                    }
                }

                // Achievements / Certifications
                if (view.getAchievements() != null && !view.getAchievements().isEmpty()) {
                    y = writeSectionHeader(cs, page, margin, y, "Achievements & Certifications");
                    for (var item : view.getAchievements()) {
                        String text = safe(item.getTitle());
                        if (item.getIssuer() != null) {
                            text += " - " + safe(item.getIssuer());
                        }
                        if (item.getDate() != null) {
                            text += " (" + safe(item.getDate()) + ")";
                        }
                        if (item.getDetails() != null) {
                            text += ": " + safe(item.getDetails());
                        }
                        y = writeBullet(cs, page, margin + 10, y, text, lineHeight, 95);
                    }
                }

                // Other content fallback
                if (view.getOtherContent() != null && !view.getOtherContent().isBlank()) {
                    y = writeSectionHeader(cs, page, margin, y, "Additional Information");
                    y = writeParagraph(cs, page, margin, y, view.getOtherContent(), lineHeight, 95);
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.save(bos);
            return bos.toByteArray();
        }
    }

    private float writeHeader(PDPageContentStream cs, PDPage page, StructuredResumeView view,
                              float margin, float y, ResumeTemplateStyle style) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        cs.newLineAtOffset(margin, y);
        String name = view.getHeader() != null && view.getHeader().getFullName() != null
                ? view.getHeader().getFullName()
                : "";
        cs.showText(safe(name));
        cs.endText();
        y -= 20;

        StringBuilder contact = new StringBuilder();
        if (view.getHeader() != null) {
            if (view.getHeader().getEmail() != null) {
                contact.append(view.getHeader().getEmail());
            }
            if (view.getHeader().getPhone() != null) {
                if (!contact.isEmpty()) contact.append(" | ");
                contact.append(view.getHeader().getPhone());
            }
            if (view.getHeader().getLocation() != null) {
                if (!contact.isEmpty()) contact.append(" | ");
                contact.append(view.getHeader().getLocation());
            }
            if (view.getHeader().getLinkedin() != null) {
                if (!contact.isEmpty()) contact.append(" | ");
                contact.append(view.getHeader().getLinkedin());
            } else if (view.getHeader().getWebsite() != null) {
                if (!contact.isEmpty()) contact.append(" | ");
                contact.append(view.getHeader().getWebsite());
            }
        }

        if (!contact.isEmpty()) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.newLineAtOffset(margin, y);
            cs.showText(safe(contact.toString()));
            cs.endText();
            y -= 18;
        } else {
            y -= 10;
        }

        return y;
    }

    private float writeSectionHeader(PDPageContentStream cs, PDPage page,
                                     float margin, float y, String title) throws IOException {
        y = ensureSpace(page, cs, margin, y, 30);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(margin, y);
        cs.showText(safe(title));
        cs.endText();
        y -= 4;
        cs.moveTo(margin, y);
        cs.lineTo(page.getMediaBox().getWidth() - margin, y);
        cs.setLineWidth(0.5f);
        cs.stroke();
        y -= 10;
        return y;
    }

    private float writeParagraph(PDPageContentStream cs, PDPage page,
                                 float margin, float y, String text,
                                 float lineHeight, int maxChars) throws IOException {
        String[] words = safe(text).split("\\s+");
        StringBuilder line = new StringBuilder();
        cs.setFont(PDType1Font.HELVETICA, 10);

        for (String w : words) {
            if (line.length() + w.length() + 1 > maxChars) {
                y = ensureSpace(page, cs, margin, y, lineHeight * 2);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(line.toString());
                cs.endText();
                y -= lineHeight;
                line = new StringBuilder();
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(w);
        }
        if (!line.isEmpty()) {
            y = ensureSpace(page, cs, margin, y, lineHeight * 2);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText(line.toString());
            cs.endText();
            y -= lineHeight;
        }
        y -= 4;
        return y;
    }

    private float writeBullet(PDPageContentStream cs, PDPage page,
                              float margin, float y, String text,
                              float lineHeight, int maxChars) throws IOException {
        String safeText = safe(text);
        String[] words = safeText.split("\\s+");
        StringBuilder line = new StringBuilder("•");

        cs.setFont(PDType1Font.HELVETICA, 10);

        for (String w : words) {
            if (line.length() + w.length() + 1 > maxChars) {
                y = ensureSpace(page, cs, margin, y, lineHeight * 2);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(line.toString());
                cs.endText();
                y -= lineHeight;
                line = new StringBuilder("  ");
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(w);
        }

        if (!line.isEmpty()) {
            y = ensureSpace(page, cs, margin, y, lineHeight * 2);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText(line.toString());
            cs.endText();
            y -= lineHeight;
        }
        return y;
    }

    private float writeBoldLine(PDPageContentStream cs, PDPage page,
                                float margin, float y, String text) throws IOException {
        y = ensureSpace(page, cs, margin, y, 20);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(margin, y);
        cs.showText(safe(text));
        cs.endText();
        y -= 14;
        return y;
    }

    private float ensureSpace(PDPage page, PDPageContentStream cs,
                              float margin, float y, float needed) {
        if (y - needed < margin) {
            // In this simplified version we assume single page; caller can extend to multipage if needed.
            return y;
        }
        return y;
    }

    private float ensureSpace(PDDocument doc, PDPageContentStream cs, PDPage page,
                              float margin, float y, float needed) {
        if (y - needed < margin) {
            return y;
        }
        return y;
    }

    private String safe(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u25E6' || c == '\u2022') {
                sb.append('-');
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
}


