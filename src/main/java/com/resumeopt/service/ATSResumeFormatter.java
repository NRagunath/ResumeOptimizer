package com.resumeopt.service;

import com.resumeopt.model.ResumeTemplateStyle;
import com.resumeopt.model.StructuredResumeView;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Formats structured resume data into ATS-safe, single-column text output.
 * 
 * ATS-SAFE REQUIREMENTS:
 * - Single-column layout
 * - No graphics, icons, tables, text boxes, headers, footers, or images
 * - Standard fonts (Arial, Calibri, Helvetica)
 * - Consistent spacing and margins
 * - Clean, parsable text structure
 * - No markdown or special formatting characters
 */
@Service
public class ATSResumeFormatter {

    /**
     * Formats structured resume into ATS-safe plain text
     */
    public String formatAsText(StructuredResumeView resume, ResumeTemplateStyle style) {
        StringBuilder sb = new StringBuilder();

        // Header: Full Name and Contact Details
        formatHeader(resume.getHeader(), sb);

        // Professional Summary
        if (resume.getSummary() != null && !resume.getSummary().trim().isEmpty()) {
            sb.append("\nPROFESSIONAL SUMMARY\n");
            sb.append(formatSectionContent(resume.getSummary(), style));
            sb.append("\n");
        }

        // Skills Section
        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            sb.append("\nTECHNICAL SKILLS\n");
            formatSkills(resume.getSkills(), sb, style);
            sb.append("\n");
        }

        // Professional Experience (reverse-chronological)
        if (resume.getExperience() != null && !resume.getExperience().isEmpty()) {
            sb.append("\nPROFESSIONAL EXPERIENCE\n");
            formatExperience(resume.getExperience(), sb, style);
            sb.append("\n");
        }

        // Projects
        if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
            sb.append("\nPROJECTS\n");
            formatProjects(resume.getProjects(), sb, style);
            sb.append("\n");
        }

        // Education
        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
            sb.append("\nEDUCATION\n");
            formatEducation(resume.getEducation(), sb, style);
            sb.append("\n");
        }

        // Certifications and Achievements
        if (resume.getAchievements() != null && !resume.getAchievements().isEmpty()) {
            sb.append("\nCERTIFICATIONS AND ACHIEVEMENTS\n");
            formatAchievements(resume.getAchievements(), sb, style);
            sb.append("\n");
        }

        // Other content (if any)
        if (resume.getOtherContent() != null && !resume.getOtherContent().trim().isEmpty()) {
            sb.append("\n").append(resume.getOtherContent()).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Formats header section with full name and contact details
     */
    private void formatHeader(StructuredResumeView.Header header, StringBuilder sb) {
        if (header == null) return;

        // Full Name (centered conceptually, but ATS-safe means just on its own line)
        if (header.getFullName() != null && !header.getFullName().trim().isEmpty()) {
            sb.append(header.getFullName().trim().toUpperCase());
            sb.append("\n");
        }

        // Contact details on single line
        List<String> contactParts = new java.util.ArrayList<>();
        if (header.getEmail() != null && !header.getEmail().trim().isEmpty()) {
            contactParts.add(header.getEmail().trim());
        }
        if (header.getPhone() != null && !header.getPhone().trim().isEmpty()) {
            contactParts.add(header.getPhone().trim());
        }
        if (header.getLocation() != null && !header.getLocation().trim().isEmpty()) {
            contactParts.add(header.getLocation().trim());
        }
        if (header.getLinkedin() != null && !header.getLinkedin().trim().isEmpty()) {
            contactParts.add(header.getLinkedin().trim());
        }
        if (header.getWebsite() != null && !header.getWebsite().trim().isEmpty()) {
            contactParts.add(header.getWebsite().trim());
        }

        if (!contactParts.isEmpty()) {
            sb.append(String.join(" | ", contactParts));
            sb.append("\n");
        }
    }

    /**
     * Formats skills section with grouped skills
     */
    private void formatSkills(List<StructuredResumeView.SkillGroup> skillGroups, StringBuilder sb, ResumeTemplateStyle style) {
        for (StructuredResumeView.SkillGroup group : skillGroups) {
            if (group.getSkills() == null || group.getSkills().isEmpty()) {
                continue;
            }

            // Group name if specified (for grouped skills)
            if (group.getGroupName() != null && 
                !group.getGroupName().trim().isEmpty() && 
                !group.getGroupName().equalsIgnoreCase("Skills") &&
                !group.getGroupName().equalsIgnoreCase("Technical Skills")) {
                sb.append(group.getGroupName().trim()).append(": ");
            }

            // Skills as comma-separated list
            String skillsList = String.join(", ", group.getSkills());
            sb.append(skillsList);
            sb.append("\n");
        }
    }

    /**
     * Formats professional experience section (reverse-chronological)
     */
    private void formatExperience(List<StructuredResumeView.ExperienceItem> experience, StringBuilder sb, ResumeTemplateStyle style) {
        for (StructuredResumeView.ExperienceItem exp : experience) {
            // Job Title
            if (exp.getTitle() != null && !exp.getTitle().trim().isEmpty()) {
                sb.append(exp.getTitle().trim());
            }

            // Company and Location
            List<String> companyInfo = new java.util.ArrayList<>();
            if (exp.getCompany() != null && !exp.getCompany().trim().isEmpty()) {
                companyInfo.add(exp.getCompany().trim());
            }
            if (exp.getLocation() != null && !exp.getLocation().trim().isEmpty()) {
                companyInfo.add(exp.getLocation().trim());
            }

            if (!companyInfo.isEmpty()) {
                sb.append(" | ").append(String.join(", ", companyInfo));
            }

            // Dates
            if (exp.getStartDate() != null || exp.getEndDate() != null) {
                sb.append(" | ");
                if (exp.getStartDate() != null && !exp.getStartDate().trim().isEmpty()) {
                    sb.append(exp.getStartDate().trim());
                }
                if (exp.getEndDate() != null && !exp.getEndDate().trim().isEmpty()) {
                    if (exp.getStartDate() != null && !exp.getStartDate().trim().isEmpty()) {
                        sb.append(" - ");
                    }
                    sb.append(exp.getEndDate().trim());
                } else {
                    sb.append(" - Present");
                }
            }

            sb.append("\n");

            // Bullet points (action-verb led, active voice)
            if (exp.getBullets() != null && !exp.getBullets().isEmpty()) {
                for (String bullet : exp.getBullets()) {
                    String formattedBullet = formatBulletPoint(bullet);
                    sb.append("  ").append(formattedBullet).append("\n");
                }
            }

            sb.append("\n");
        }
    }

    /**
     * Formats projects section
     */
    private void formatProjects(List<StructuredResumeView.ProjectItem> projects, StringBuilder sb, ResumeTemplateStyle style) {
        for (StructuredResumeView.ProjectItem project : projects) {
            // Project Name
            if (project.getName() != null && !project.getName().trim().isEmpty()) {
                sb.append(project.getName().trim());
            }

            // Role and Dates
            List<String> projectInfo = new java.util.ArrayList<>();
            if (project.getRole() != null && !project.getRole().trim().isEmpty()) {
                projectInfo.add(project.getRole().trim());
            }
            if (project.getDates() != null && !project.getDates().trim().isEmpty()) {
                projectInfo.add(project.getDates().trim());
            }

            if (!projectInfo.isEmpty()) {
                sb.append(" | ").append(String.join(" | ", projectInfo));
            }

            sb.append("\n");

            // Bullet points
            if (project.getBullets() != null && !project.getBullets().isEmpty()) {
                for (String bullet : project.getBullets()) {
                    String formattedBullet = formatBulletPoint(bullet);
                    sb.append("  ").append(formattedBullet).append("\n");
                }
            }

            sb.append("\n");
        }
    }

    /**
     * Formats education section
     */
    private void formatEducation(List<StructuredResumeView.EducationItem> education, StringBuilder sb, ResumeTemplateStyle style) {
        for (StructuredResumeView.EducationItem edu : education) {
            // Degree
            if (edu.getDegree() != null && !edu.getDegree().trim().isEmpty()) {
                sb.append(edu.getDegree().trim());
            }

            // Institution and Field of Study
            List<String> eduInfo = new java.util.ArrayList<>();
            if (edu.getInstitution() != null && !edu.getInstitution().trim().isEmpty()) {
                eduInfo.add(edu.getInstitution().trim());
            }
            if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().trim().isEmpty()) {
                eduInfo.add(edu.getFieldOfStudy().trim());
            }

            if (!eduInfo.isEmpty()) {
                sb.append(" | ").append(String.join(", ", eduInfo));
            }

            // Dates
            if (edu.getDates() != null && !edu.getDates().trim().isEmpty()) {
                sb.append(" | ").append(edu.getDates().trim());
            }

            sb.append("\n");

            // Additional bullets (honors, GPA, etc.)
            if (edu.getBullets() != null && !edu.getBullets().isEmpty()) {
                for (String bullet : edu.getBullets()) {
                    sb.append("  ").append(bullet.trim()).append("\n");
                }
            }

            sb.append("\n");
        }
    }

    /**
     * Formats achievements/certifications section
     */
    private void formatAchievements(List<StructuredResumeView.AchievementItem> achievements, StringBuilder sb, ResumeTemplateStyle style) {
        for (StructuredResumeView.AchievementItem achievement : achievements) {
            // Title
            if (achievement.getTitle() != null && !achievement.getTitle().trim().isEmpty()) {
                sb.append(achievement.getTitle().trim());
            }

            // Issuer and Date
            List<String> achInfo = new java.util.ArrayList<>();
            if (achievement.getIssuer() != null && !achievement.getIssuer().trim().isEmpty()) {
                achInfo.add(achievement.getIssuer().trim());
            }
            if (achievement.getDate() != null && !achievement.getDate().trim().isEmpty()) {
                achInfo.add(achievement.getDate().trim());
            }

            if (!achInfo.isEmpty()) {
                sb.append(" | ").append(String.join(" | ", achInfo));
            }

            sb.append("\n");

            // Details
            if (achievement.getDetails() != null && !achievement.getDetails().trim().isEmpty()) {
                sb.append("  ").append(achievement.getDetails().trim()).append("\n");
            }

            sb.append("\n");
        }
    }

    /**
     * Formats a bullet point ensuring it starts with an action verb and uses active voice
     */
    private String formatBulletPoint(String bullet) {
        String formatted = bullet.trim();

        // Remove bullet markers if present
        formatted = formatted.replaceFirst("^[\\-*•]\\s*", "");

        // Ensure it doesn't start with first-person pronouns
        formatted = formatted.replaceFirst("^(?i)(I|We|My|Our)\\s+", "");

        // Capitalize first letter
        if (!formatted.isEmpty()) {
            formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        }

        // Ensure it ends with proper punctuation
        if (!formatted.isEmpty() && !formatted.endsWith(".") && !formatted.endsWith("!") && !formatted.endsWith("?")) {
            formatted += ".";
        }

        return formatted;
    }

    /**
     * Formats section content with proper line breaks
     */
    private String formatSectionContent(String content, ResumeTemplateStyle style) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        // Split into sentences and format
        String[] sentences = content.split("(?<=[.!?])\\s+");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                // Capitalize first letter
                if (sentence.length() > 0) {
                    sentence = sentence.substring(0, 1).toUpperCase() + sentence.substring(1);
                }
                formatted.append(sentence);
                if (i < sentences.length - 1) {
                    formatted.append(" ");
                }
            }
        }

        return formatted.toString();
    }
}

