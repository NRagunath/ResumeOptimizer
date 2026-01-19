// This is a temporary file - will replace BrowserPdfGenerator.java
// Complete implementation with PageContext pattern for all methods

package com.resumeopt.service;

import com.resumeopt.model.ResumeTemplateStyle;
import com.resumeopt.model.StructuredResumeView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class BrowserPdfGenerator_Fixed {

    @Autowired
    private ATSResumeFormatter formatter;

    private static final PDType1Font HEADER_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font SECTION_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font BODY_FONT = PDType1Font.HELVETICA;
    private static final PDType1Font BULLET_FONT = PDType1Font.HELVETICA;

    private static final float HEADER_FONT_SIZE = 16f;
    private static final float SECTION_FONT_SIZE = 12f;
    private static final float BODY_FONT_SIZE = 10f;
    private static final float BULLET_FONT_SIZE = 10f;

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 12f;
    private static final float SECTION_SPACING = 8f;
    private static final float BULLET_INDENT = 20f;

    public byte[] generatePdf(StructuredResumeView resume, ResumeTemplateStyle style) throws IOException {
        PDDocument document = new PDDocument();
        PageContext context = new PageContext(document);
        
        try {
            renderHeader(resume.getHeader(), context);
            if (resume.getSummary() != null && !resume.getSummary().trim().isEmpty()) {
                renderSection("PROFESSIONAL SUMMARY", resume.getSummary(), context);
            }
            if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
                renderSkillsSection(resume.getSkills(), context);
            }
            if (resume.getExperience() != null && !resume.getExperience().isEmpty()) {
                renderExperienceSection(resume.getExperience(), context);
            }
            if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
                renderProjectsSection(resume.getProjects(), context);
            }
            if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
                renderEducationSection(resume.getEducation(), context);
            }
            if (resume.getAchievements() != null && !resume.getAchievements().isEmpty()) {
                renderAchievementsSection(resume.getAchievements(), context);
            }

            if (context.contentStream != null) {
                context.contentStream.close();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            document.close();
            throw e;
        }
    }

    private static class PageContext {
        PDDocument document;
        PDPage currentPage;
        PDPageContentStream contentStream;
        float y;
        float pageWidth;
        float contentWidth;

        PageContext(PDDocument document) throws IOException {
            this.document = document;
            this.pageWidth = PDRectangle.LETTER.getWidth();
            this.contentWidth = pageWidth - (2 * MARGIN);
            newPage();
        }

        void newPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            currentPage = new PDPage(PDRectangle.LETTER);
            document.addPage(currentPage);
            contentStream = new PDPageContentStream(document, currentPage);
            y = currentPage.getMediaBox().getHeight() - MARGIN;
        }

        void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < MARGIN + 50) {
                newPage();
            }
        }
    }

    private void renderHeader(StructuredResumeView.Header header, PageContext ctx) throws IOException {
        if (header == null) return;
        if (header.getFullName() != null && !header.getFullName().trim().isEmpty()) {
            ctx.contentStream.beginText();
            ctx.contentStream.setFont(HEADER_FONT, HEADER_FONT_SIZE);
            String name = header.getFullName().trim().toUpperCase();
            float nameWidth = HEADER_FONT_SIZE * name.length() * 0.6f;
            float nameX = MARGIN + (ctx.contentWidth - nameWidth) / 2;
            ctx.contentStream.newLineAtOffset(nameX, ctx.y);
            ctx.contentStream.showText(sanitizeForPdf(name));
            ctx.contentStream.endText();
            ctx.y -= HEADER_FONT_SIZE + 8;
        }
        List<String> contactParts = new java.util.ArrayList<>();
        if (header.getEmail() != null && !header.getEmail().trim().isEmpty()) contactParts.add(header.getEmail().trim());
        if (header.getPhone() != null && !header.getPhone().trim().isEmpty()) contactParts.add(header.getPhone().trim());
        if (header.getLocation() != null && !header.getLocation().trim().isEmpty()) contactParts.add(header.getLocation().trim());
        if (header.getLinkedin() != null && !header.getLinkedin().trim().isEmpty()) contactParts.add(header.getLinkedin().trim());
        if (header.getWebsite() != null && !header.getWebsite().trim().isEmpty()) contactParts.add(header.getWebsite().trim());
        if (!contactParts.isEmpty()) {
            String contactLine = String.join(" | ", contactParts);
            ctx.contentStream.beginText();
            ctx.contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
            ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
            ctx.contentStream.showText(sanitizeForPdf(contactLine));
            ctx.contentStream.endText();
            ctx.y -= BODY_FONT_SIZE + SECTION_SPACING;
        }
    }

    private void renderSection(String title, String content, PageContext ctx) throws IOException {
        ctx.ensureSpace(SECTION_FONT_SIZE + 20);
        ctx.contentStream.beginText();
        ctx.contentStream.setFont(SECTION_FONT, SECTION_FONT_SIZE);
        ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
        ctx.contentStream.showText(sanitizeForPdf(title));
        ctx.contentStream.endText();
        ctx.y -= SECTION_FONT_SIZE + 4;
        ctx.contentStream.moveTo(MARGIN, ctx.y);
        ctx.contentStream.lineTo(MARGIN + 150, ctx.y);
        ctx.contentStream.setLineWidth(0.5f);
        ctx.contentStream.stroke();
        ctx.y -= 4;
        renderWrappedText(content, ctx, BODY_FONT, BODY_FONT_SIZE);
        ctx.y -= SECTION_SPACING;
    }

    private void renderSkillsSection(List<StructuredResumeView.SkillGroup> skillGroups, PageContext ctx) throws IOException {
        ctx.ensureSpace(SECTION_FONT_SIZE + 20);
        ctx.contentStream.beginText();
        ctx.contentStream.setFont(SECTION_FONT, SECTION_FONT_SIZE);
        ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
        ctx.contentStream.showText("TECHNICAL SKILLS");
        ctx.contentStream.endText();
        ctx.y -= SECTION_FONT_SIZE + 4;
        ctx.contentStream.moveTo(MARGIN, ctx.y);
        ctx.contentStream.lineTo(MARGIN + 150, ctx.y);
        ctx.contentStream.setLineWidth(0.5f);
        ctx.contentStream.stroke();
        ctx.y -= 4;
        for (StructuredResumeView.SkillGroup group : skillGroups) {
            if (group.getSkills() == null || group.getSkills().isEmpty()) continue;
            String skillsLine = (group.getGroupName() != null && !group.getGroupName().trim().isEmpty() && 
                !group.getGroupName().equalsIgnoreCase("Skills") && !group.getGroupName().equalsIgnoreCase("Technical Skills"))
                ? group.getGroupName().trim() + ": " + String.join(", ", group.getSkills())
                : String.join(", ", group.getSkills());
            renderWrappedText(skillsLine, ctx, BODY_FONT, BODY_FONT_SIZE);
        }
        ctx.y -= SECTION_SPACING;
    }

    private void renderExperienceSection(List<StructuredResumeView.ExperienceItem> experience, PageContext ctx) throws IOException {
        ctx.ensureSpace(SECTION_FONT_SIZE + 20);
        ctx.contentStream.beginText();
        ctx.contentStream.setFont(SECTION_FONT, SECTION_FONT_SIZE);
        ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
        ctx.contentStream.showText("PROFESSIONAL EXPERIENCE");
        ctx.contentStream.endText();
        ctx.y -= SECTION_FONT_SIZE + 4;
        ctx.contentStream.moveTo(MARGIN, ctx.y);
        ctx.contentStream.lineTo(MARGIN + 150, ctx.y);
        ctx.contentStream.setLineWidth(0.5f);
        ctx.contentStream.stroke();
        ctx.y -= 4;
        for (StructuredResumeView.ExperienceItem exp : experience) {
            ctx.ensureSpace(BODY_FONT_SIZE + 20);
            StringBuilder jobLine = new StringBuilder();
            if (exp.getTitle() != null && !exp.getTitle().trim().isEmpty()) jobLine.append(exp.getTitle().trim());
            List<String> companyInfo = new java.util.ArrayList<>();
            if (exp.getCompany() != null && !exp.getCompany().trim().isEmpty()) companyInfo.add(exp.getCompany().trim());
            if (exp.getLocation() != null && !exp.getLocation().trim().isEmpty()) companyInfo.add(exp.getLocation().trim());
            if (!companyInfo.isEmpty()) jobLine.append(" | ").append(String.join(", ", companyInfo));
            if (exp.getStartDate() != null || exp.getEndDate() != null) {
                jobLine.append(" | ");
                if (exp.getStartDate() != null && !exp.getStartDate().trim().isEmpty()) jobLine.append(exp.getStartDate().trim());
                if (exp.getEndDate() != null && !exp.getEndDate().trim().isEmpty()) {
                    if (exp.getStartDate() != null && !exp.getStartDate().trim().isEmpty()) jobLine.append(" - ");
                    jobLine.append(exp.getEndDate().trim());
                } else jobLine.append(" - Present");
            }
            ctx.contentStream.beginText();
            ctx.contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
            ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
            ctx.contentStream.showText(sanitizeForPdf(jobLine.toString()));
            ctx.contentStream.endText();
            ctx.y -= BODY_FONT_SIZE + 4;
            if (exp.getBullets() != null && !exp.getBullets().isEmpty()) {
                for (String bullet : exp.getBullets()) {
                    ctx.ensureSpace(BULLET_FONT_SIZE + 10);
                    renderWrappedText("  " + formatBulletPoint(bullet), ctx, BULLET_FONT, BULLET_FONT_SIZE);
                }
            }
            ctx.y -= 4;
        }
        ctx.y -= SECTION_SPACING;
    }

    private void renderProjectsSection(List<StructuredResumeView.ProjectItem> projects, PageContext ctx) throws IOException {
        ctx.ensureSpace(SECTION_FONT_SIZE + 20);
        ctx.contentStream.beginText();
        ctx.contentStream.setFont(SECTION_FONT, SECTION_FONT_SIZE);
        ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
        ctx.contentStream.showText("PROJECTS");
        ctx.contentStream.endText();
        ctx.y -= SECTION_FONT_SIZE + 4;
        ctx.contentStream.moveTo(MARGIN, ctx.y);
        ctx.contentStream.lineTo(MARGIN + 150, ctx.y);
        ctx.contentStream.setLineWidth(0.5f);
        ctx.contentStream.stroke();
        ctx.y -= 4;
        for (StructuredResumeView.ProjectItem project : projects) {
            ctx.ensureSpace(BODY_FONT_SIZE + 20);
            StringBuilder projectLine = new StringBuilder();
            if (project.getName() != null && !project.getName().trim().isEmpty()) projectLine.append(project.getName().trim());
            List<String> projectInfo = new java.util.ArrayList<>();
            if (project.getRole() != null && !project.getRole().trim().isEmpty()) projectInfo.add(project.getRole().trim());
            if (project.getDates() != null && !project.getDates().trim().isEmpty()) projectInfo.add(project.getDates().trim());
            if (!projectInfo.isEmpty()) projectLine.append(" | ").append(String.join(" | ", projectInfo));
            ctx.contentStream.beginText();
            ctx.contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
            ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
            ctx.contentStream.showText(sanitizeForPdf(projectLine.toString()));
            ctx.contentStream.endText();
            ctx.y -= BODY_FONT_SIZE + 4;
            if (project.getBullets() != null && !project.getBullets().isEmpty()) {
                for (String bullet : project.getBullets()) {
                    ctx.ensureSpace(BULLET_FONT_SIZE + 10);
                    renderWrappedText("  " + formatBulletPoint(bullet), ctx, BULLET_FONT, BULLET_FONT_SIZE);
                }
            }
            ctx.y -= 4;
        }
        ctx.y -= SECTION_SPACING;
    }

    private void renderEducationSection(List<StructuredResumeView.EducationItem> education, PageContext ctx) throws IOException {
        ctx.ensureSpace(SECTION_FONT_SIZE + 20);
        ctx.contentStream.beginText();
        ctx.contentStream.setFont(SECTION_FONT, SECTION_FONT_SIZE);
        ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
        ctx.contentStream.showText("EDUCATION");
        ctx.contentStream.endText();
        ctx.y -= SECTION_FONT_SIZE + 4;
        ctx.contentStream.moveTo(MARGIN, ctx.y);
        ctx.contentStream.lineTo(MARGIN + 150, ctx.y);
        ctx.contentStream.setLineWidth(0.5f);
        ctx.contentStream.stroke();
        ctx.y -= 4;
        for (StructuredResumeView.EducationItem edu : education) {
            ctx.ensureSpace(BODY_FONT_SIZE + 20);
            StringBuilder eduLine = new StringBuilder();
            if (edu.getDegree() != null && !edu.getDegree().trim().isEmpty()) eduLine.append(edu.getDegree().trim());
            List<String> eduInfo = new java.util.ArrayList<>();
            if (edu.getInstitution() != null && !edu.getInstitution().trim().isEmpty()) eduInfo.add(edu.getInstitution().trim());
            if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().trim().isEmpty()) eduInfo.add(edu.getFieldOfStudy().trim());
            if (!eduInfo.isEmpty()) eduLine.append(" | ").append(String.join(", ", eduInfo));
            if (edu.getDates() != null && !edu.getDates().trim().isEmpty()) eduLine.append(" | ").append(edu.getDates().trim());
            ctx.contentStream.beginText();
            ctx.contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
            ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
            ctx.contentStream.showText(sanitizeForPdf(eduLine.toString()));
            ctx.contentStream.endText();
            ctx.y -= BODY_FONT_SIZE + 4;
            if (edu.getBullets() != null && !edu.getBullets().isEmpty()) {
                for (String bullet : edu.getBullets()) {
                    ctx.ensureSpace(BULLET_FONT_SIZE + 10);
                    renderWrappedText("  " + bullet.trim(), ctx, BULLET_FONT, BULLET_FONT_SIZE);
                }
            }
            ctx.y -= 4;
        }
        ctx.y -= SECTION_SPACING;
    }

    private void renderAchievementsSection(List<StructuredResumeView.AchievementItem> achievements, PageContext ctx) throws IOException {
        ctx.ensureSpace(SECTION_FONT_SIZE + 20);
        ctx.contentStream.beginText();
        ctx.contentStream.setFont(SECTION_FONT, SECTION_FONT_SIZE);
        ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
        ctx.contentStream.showText("CERTIFICATIONS AND ACHIEVEMENTS");
        ctx.contentStream.endText();
        ctx.y -= SECTION_FONT_SIZE + 4;
        ctx.contentStream.moveTo(MARGIN, ctx.y);
        ctx.contentStream.lineTo(MARGIN + 150, ctx.y);
        ctx.contentStream.setLineWidth(0.5f);
        ctx.contentStream.stroke();
        ctx.y -= 4;
        for (StructuredResumeView.AchievementItem achievement : achievements) {
            ctx.ensureSpace(BODY_FONT_SIZE + 20);
            StringBuilder achLine = new StringBuilder();
            if (achievement.getTitle() != null && !achievement.getTitle().trim().isEmpty()) achLine.append(achievement.getTitle().trim());
            List<String> achInfo = new java.util.ArrayList<>();
            if (achievement.getIssuer() != null && !achievement.getIssuer().trim().isEmpty()) achInfo.add(achievement.getIssuer().trim());
            if (achievement.getDate() != null && !achievement.getDate().trim().isEmpty()) achInfo.add(achievement.getDate().trim());
            if (!achInfo.isEmpty()) achLine.append(" | ").append(String.join(" | ", achInfo));
            ctx.contentStream.beginText();
            ctx.contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
            ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
            ctx.contentStream.showText(sanitizeForPdf(achLine.toString()));
            ctx.contentStream.endText();
            ctx.y -= BODY_FONT_SIZE + 4;
            if (achievement.getDetails() != null && !achievement.getDetails().trim().isEmpty()) {
                renderWrappedText("  " + achievement.getDetails().trim(), ctx, BULLET_FONT, BULLET_FONT_SIZE);
            }
            ctx.y -= 4;
        }
        ctx.y -= SECTION_SPACING;
    }

    private void renderWrappedText(String text, PageContext ctx, PDType1Font font, float fontSize) throws IOException {
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            float testWidth = fontSize * testLine.length() * 0.6f;
            if (testWidth > ctx.contentWidth && currentLine.length() > 0) {
                ctx.ensureSpace(fontSize + 2);
                ctx.contentStream.beginText();
                ctx.contentStream.setFont(font, fontSize);
                ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
                ctx.contentStream.showText(sanitizeForPdf(currentLine.toString()));
                ctx.contentStream.endText();
                ctx.y -= fontSize + 2;
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            ctx.ensureSpace(fontSize + 2);
            ctx.contentStream.beginText();
            ctx.contentStream.setFont(font, fontSize);
            ctx.contentStream.newLineAtOffset(MARGIN, ctx.y);
            ctx.contentStream.showText(sanitizeForPdf(currentLine.toString()));
            ctx.contentStream.endText();
            ctx.y -= fontSize + 2;
        }
    }

    private String formatBulletPoint(String bullet) {
        String formatted = bullet.trim();
        formatted = formatted.replaceFirst("^[\\-*•]\\s*", "");
        formatted = formatted.replaceFirst("^(?i)(I|We|My|Our)\\s+", "");
        if (!formatted.isEmpty()) {
            formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        }
        if (!formatted.isEmpty() && !formatted.endsWith(".") && !formatted.endsWith("!") && !formatted.endsWith("?")) {
            formatted += ".";
        }
        return formatted;
    }

    private String sanitizeForPdf(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 32 && c <= 126) {
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

