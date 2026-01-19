package com.resumeopt.service;

import com.resumeopt.model.StructuredResumeView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic parser that converts flat resume text into a StructuredResumeView.
 * Uses simple regex/keywords to split major sections while preserving all content.
 */
@Service
public class ResumeStructuringService {

    private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile(
            "^(?i)(summary|professional summary|objective|profile|skills|technical skills|experience|work experience|professional experience|projects|personal projects|education|academic background|achievements|certifications)\\b.*$");

    public StructuredResumeView buildView(String optimizedText) {
        if (optimizedText == null) {
            optimizedText = "";
        }

        StructuredResumeView view = new StructuredResumeView();

        // Basic high-level split into sections by headings
        List<Section> sections = splitIntoSections(optimizedText);
        StringBuilder otherBuffer = new StringBuilder();

        for (Section section : sections) {
            String header = section.header.toLowerCase();
            String body = section.body.trim();

            if (header.matches("summary|professional summary|objective|profile")) {
                view.setSummary(body);
            } else if (header.contains("skill")) {
                view.setSkills(parseSkills(body));
            } else if (header.contains("experience") && !header.contains("project")) {
                view.setExperience(parseExperience(body));
            } else if (header.contains("project")) {
                view.setProjects(parseProjects(body));
            } else if (header.contains("education") || header.contains("academic")) {
                view.setEducation(parseEducation(body));
            } else if (header.contains("achievement") || header.contains("certification")) {
                view.setAchievements(parseAchievements(body));
            } else {
                if (!body.isEmpty()) {
                    otherBuffer.append(section.header).append("\n").append(body).append("\n\n");
                }
            }
        }

        // Attempt simple header/contact extraction from very top of resume text
        view.setHeader(parseHeader(optimizedText));

        if (otherBuffer.length() == 0 && sections.isEmpty()) {
            view.setOtherContent(optimizedText);
        } else {
            // Include any remaining content that wasn't cleanly mapped
            view.setOtherContent(otherBuffer.toString().trim());
        }

        return view;
    }

    private static class Section {
        String header;
        String body;
    }

    private List<Section> splitIntoSections(String text) {
        List<Section> sections = new ArrayList<>();
        String[] lines = text.split("\\R");

        String currentHeader = "Header";
        StringBuilder currentBody = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                currentBody.append(line).append("\n");
                continue;
            }

            Matcher m = SECTION_HEADER_PATTERN.matcher(trimmed);
            if (m.matches()) {
                // flush previous section
                Section s = new Section();
                s.header = currentHeader;
                s.body = currentBody.toString();
                sections.add(s);

                currentHeader = m.group(1);
                currentBody = new StringBuilder();
            } else {
                currentBody.append(line).append("\n");
            }
        }

        Section last = new Section();
        last.header = currentHeader;
        last.body = currentBody.toString();
        sections.add(last);

        return sections;
    }

    private StructuredResumeView.Header parseHeader(String text) {
        StructuredResumeView.Header header = new StructuredResumeView.Header();
        String[] lines = text.split("\\R", 5);
        if (lines.length > 0) {
            header.setFullName(lines[0].trim());
        }

        Pattern emailPattern = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
        Pattern phonePattern = Pattern.compile("\\+?\\d[\\d\\s().-]{7,}");
        Pattern linkedinPattern = Pattern.compile("https?://(www\\.)?linkedin\\.com/[^\\s]+", Pattern.CASE_INSENSITIVE);
        Pattern urlPattern = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);

        Matcher m = emailPattern.matcher(text);
        if (m.find()) {
            header.setEmail(m.group());
        }

        m = phonePattern.matcher(text);
        if (m.find()) {
            header.setPhone(m.group());
        }

        m = linkedinPattern.matcher(text);
        if (m.find()) {
            header.setLinkedin(m.group());
        } else {
            m = urlPattern.matcher(text);
            if (m.find()) {
                header.setWebsite(m.group());
            }
        }

        return header;
    }

    private List<StructuredResumeView.SkillGroup> parseSkills(String body) {
        List<StructuredResumeView.SkillGroup> groups = new ArrayList<>();
        StructuredResumeView.SkillGroup defaultGroup = new StructuredResumeView.SkillGroup();
        defaultGroup.setGroupName("Skills");

        String[] lines = body.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // line like "Programming: Java, Python"
            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":", 2);
                StructuredResumeView.SkillGroup g = new StructuredResumeView.SkillGroup();
                g.setGroupName(parts[0].trim());
                for (String s : parts[1].split("[,;]")) {
                    String skill = s.trim();
                    if (!skill.isEmpty()) {
                        g.getSkills().add(skill);
                    }
                }
                if (!g.getSkills().isEmpty()) {
                    groups.add(g);
                }
            } else {
                for (String s : trimmed.split("[,;]")) {
                    String skill = s.trim();
                    if (!skill.isEmpty()) {
                        defaultGroup.getSkills().add(skill);
                    }
                }
            }
        }

        if (!defaultGroup.getSkills().isEmpty()) {
            groups.add(defaultGroup);
        }
        return groups;
    }

    private List<StructuredResumeView.ExperienceItem> parseExperience(String body) {
        List<StructuredResumeView.ExperienceItem> list = new ArrayList<>();
        String[] blocks = body.split("\\n\\s*\\n"); // paragraph blocks
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;
            StructuredResumeView.ExperienceItem item = new StructuredResumeView.ExperienceItem();

            String[] lines = trimmed.split("\\R");
            if (lines.length > 0) {
                item.setTitle(lines[0].trim());
            }
            if (lines.length > 1) {
                item.setCompany(lines[1].trim());
            }

            // bullets: lines starting with -, *, • or split sentences
            List<String> bullets = new ArrayList<>();
            for (int i = 2; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
                    bullets.add(line.replaceFirst("^[\\-*•]\\s*", ""));
                } else {
                    for (String s : line.split("(?<=[.!?])\\s+")) {
                        String b = s.trim();
                        if (!b.isEmpty()) bullets.add(b);
                    }
                }
            }
            item.setBullets(bullets);
            list.add(item);
        }
        return list;
    }

    private List<StructuredResumeView.ProjectItem> parseProjects(String body) {
        List<StructuredResumeView.ProjectItem> list = new ArrayList<>();
        String[] blocks = body.split("\\n\\s*\\n");
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;
            StructuredResumeView.ProjectItem item = new StructuredResumeView.ProjectItem();
            String[] lines = trimmed.split("\\R");
            if (lines.length > 0) {
                item.setName(lines[0].trim());
            }

            List<String> bullets = new ArrayList<>();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
                    bullets.add(line.replaceFirst("^[\\-*•]\\s*", ""));
                } else {
                    for (String s : line.split("(?<=[.!?])\\s+")) {
                        String b = s.trim();
                        if (!b.isEmpty()) bullets.add(b);
                    }
                }
            }
            item.setBullets(bullets);
            list.add(item);
        }
        return list;
    }

    private List<StructuredResumeView.EducationItem> parseEducation(String body) {
        List<StructuredResumeView.EducationItem> list = new ArrayList<>();
        String[] blocks = body.split("\\n\\s*\\n");
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;
            StructuredResumeView.EducationItem item = new StructuredResumeView.EducationItem();
            String[] lines = trimmed.split("\\R");
            if (lines.length > 0) {
                item.setInstitution(lines[0].trim());
            }
            if (lines.length > 1) {
                item.setDegree(lines[1].trim());
            }

            List<String> bullets = new ArrayList<>();
            for (int i = 2; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                bullets.add(line);
            }
            item.setBullets(bullets);
            list.add(item);
        }
        return list;
    }

    private List<StructuredResumeView.AchievementItem> parseAchievements(String body) {
        List<StructuredResumeView.AchievementItem> list = new ArrayList<>();
        String[] lines = body.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            StructuredResumeView.AchievementItem item = new StructuredResumeView.AchievementItem();
            item.setTitle(trimmed.replaceFirst("^[\\-*•]\\s*", ""));
            list.add(item);
        }
        return list;
    }
}


