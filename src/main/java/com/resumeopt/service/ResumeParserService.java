package com.resumeopt.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ResumeParserService {
    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".pdf")) {
            try (InputStream is = file.getInputStream(); PDDocument doc = PDDocument.load(is)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true); // Sort by position to handle columns better
                return stripper.getText(doc);
            }
        } else if (filename.endsWith(".docx")) {
            try (InputStream is = file.getInputStream(); XWPFDocument doc = new XWPFDocument(is)) {
                StringBuilder sb = new StringBuilder();
                List<IBodyElement> bodyElements = doc.getBodyElements();
                for (IBodyElement element : bodyElements) {
                    if (element.getElementType() == BodyElementType.PARAGRAPH) {
                        XWPFParagraph paragraph = (XWPFParagraph) element;
                        sb.append(paragraph.getText()).append("\n");
                    } else if (element.getElementType() == BodyElementType.TABLE) {
                        XWPFTable table = (XWPFTable) element;
                        table.getRows().forEach(row -> {
                            row.getTableCells().forEach(cell -> {
                                sb.append(cell.getText()).append(" | ");
                            });
                            sb.append("\n");
                        });
                    }
                }
                return sb.toString();
            }
        }
        // fallback to plain text
        return new String(file.getBytes());
    }
    
    /**
     * Enhanced method to extract academic projects and fresher-specific information from resume text
     */
    public FresherResumeData extractFresherSpecificData(String resumeText) {
        FresherResumeData fresherData = new FresherResumeData();
        
        // Extract academic projects
        fresherData.setAcademicProjects(extractAcademicProjects(resumeText));
        
        // Extract internships
        fresherData.setInternships(extractInternships(resumeText));
        
        // Extract hackathons and coding competitions
        fresherData.setHackathons(extractHackathons(resumeText));
        
        // Extract volunteer work
        fresherData.setVolunteerWork(extractVolunteerWork(resumeText));
        
        // Extract academic achievements
        fresherData.setAcademicAchievements(extractAcademicAchievements(resumeText));
        
        // Extract GitHub profile
        fresherData.setGithubProfile(extractGithubProfile(resumeText));
        
        // Extract course work
        fresherData.setCourseWork(extractCourseWork(resumeText));
        
        return fresherData;
    }
    
    private List<String> extractAcademicProjects(String text) {
        List<String> projects = new java.util.ArrayList<>();
        String[] lines = text.split("[\n\r]+\s*");
        
        for (String line : lines) {
            line = line.toLowerCase().trim();
            
            // Look for academic project indicators
            if (line.contains("project") && 
                (line.contains("academic") || line.contains("college") || 
                 line.contains("university") || line.contains("school") || 
                 line.contains("semester") || line.contains("academic"))) {
                
                // Extract the project description
                int start = Math.max(0, text.toLowerCase().indexOf(line) - 100);
                int end = Math.min(text.length(), text.toLowerCase().indexOf(line) + line.length() + 200);
                String project = text.substring(start, end);
                
                if (!project.trim().isEmpty() && !projects.contains(project.trim())) {
                    projects.add(project.trim());
                }
            }
        }
        
        // Also look for common project section headers
        String[] projectHeaders = {"projects", "academic projects", "course projects", "engineering projects"};
        for (String header : projectHeaders) {
            int index = text.toLowerCase().indexOf(header);
            if (index != -1) {
                // Extract content after the header
                String remainingText = text.substring(index + header.length());
                String[] projectLines = remainingText.split("[\n\r]+\s*");
                
                for (int i = 0; i < Math.min(10, projectLines.length); i++) { // Limit to 10 lines
                    String projectLine = projectLines[i].trim();
                    if (!projectLine.isEmpty() && !projectLine.toLowerCase().contains("education") && 
                        !projectLine.toLowerCase().contains("skills") && 
                        !projectLine.toLowerCase().contains("experience")) {
                        if (!projects.contains(projectLine)) {
                            projects.add(projectLine);
                        }
                    }
                }
            }
        }
        
        return projects;
    }
    
    private List<String> extractInternships(String text) {
        List<String> internships = new java.util.ArrayList<>();
        String[] lines = text.split("[\n\r]+\s*");
        
        for (String line : lines) {
            line = line.toLowerCase().trim();
            
            if (line.contains("intern") || line.contains("internship")) {
                int start = Math.max(0, text.toLowerCase().indexOf(line) - 100);
                int end = Math.min(text.length(), text.toLowerCase().indexOf(line) + line.length() + 200);
                String internship = text.substring(start, end);
                
                if (!internship.trim().isEmpty() && !internships.contains(internship.trim())) {
                    internships.add(internship.trim());
                }
            }
        }
        
        return internships;
    }
    
    private List<String> extractHackathons(String text) {
        List<String> hackathons = new java.util.ArrayList<>();
        String[] lines = text.split("[\n\r]+\s*");
        
        for (String line : lines) {
            line = line.toLowerCase().trim();
            
            if (line.contains("hackathon") || line.contains("coding competition") || 
                line.contains("coding challenge") || line.contains("programming competition")) {
                int start = Math.max(0, text.toLowerCase().indexOf(line) - 100);
                int end = Math.min(text.length(), text.toLowerCase().indexOf(line) + line.length() + 200);
                String hackathon = text.substring(start, end);
                
                if (!hackathon.trim().isEmpty() && !hackathons.contains(hackathon.trim())) {
                    hackathons.add(hackathon.trim());
                }
            }
        }
        
        return hackathons;
    }
    
    private List<String> extractVolunteerWork(String text) {
        List<String> volunteerWork = new java.util.ArrayList<>();
        String[] lines = text.split("[\n\r]+\s*");
        
        for (String line : lines) {
            line = line.toLowerCase().trim();
            
            if (line.contains("volunteer") || line.contains("volunteering") || 
                line.contains("community service")) {
                int start = Math.max(0, text.toLowerCase().indexOf(line) - 100);
                int end = Math.min(text.length(), text.toLowerCase().indexOf(line) + line.length() + 200);
                String volunteer = text.substring(start, end);
                
                if (!volunteer.trim().isEmpty() && !volunteerWork.contains(volunteer.trim())) {
                    volunteerWork.add(volunteer.trim());
                }
            }
        }
        
        return volunteerWork;
    }
    
    private List<String> extractAcademicAchievements(String text) {
        List<String> achievements = new java.util.ArrayList<>();
        String[] lines = text.split("[\n\r]+\s*");
        
        for (String line : lines) {
            line = line.toLowerCase().trim();
            
            if (line.contains("achievement") || line.contains("award") || 
                line.contains("honor") || line.contains("scholarship")) {
                int start = Math.max(0, text.toLowerCase().indexOf(line) - 100);
                int end = Math.min(text.length(), text.toLowerCase().indexOf(line) + line.length() + 200);
                String achievement = text.substring(start, end);
                
                if (!achievement.trim().isEmpty() && !achievements.contains(achievement.trim())) {
                    achievements.add(achievement.trim());
                }
            }
        }
        
        return achievements;
    }
    
    private String extractGithubProfile(String text) {
        java.util.regex.Pattern githubPattern = java.util.regex.Pattern.compile(
            "(https?://github.com/[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+|github.com/[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+|[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+)"
        );
        java.util.regex.Matcher matcher = githubPattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "";
    }
    
    private List<String> extractCourseWork(String text) {
        List<String> coursework = new java.util.ArrayList<>();
        String[] lines = text.split("[\n\r]+\s*");
        
        for (String line : lines) {
            line = line.toLowerCase().trim();
            
            if (line.contains("course") || line.contains("coursework") || 
                line.contains("relevant courses")) {
                int start = Math.max(0, text.toLowerCase().indexOf(line) - 100);
                int end = Math.min(text.length(), text.toLowerCase().indexOf(line) + line.length() + 200);
                String course = text.substring(start, end);
                
                if (!course.trim().isEmpty() && !coursework.contains(course.trim())) {
                    coursework.add(course.trim());
                }
            }
        }
        
        return coursework;
    }
    
    /**
     * Inner class to hold fresher-specific resume data
     */
    public static class FresherResumeData {
        private List<String> academicProjects;
        private List<String> internships;
        private List<String> hackathons;
        private List<String> volunteerWork;
        private List<String> academicAchievements;
        private String githubProfile;
        private List<String> courseWork;
        
        public FresherResumeData() {
            this.academicProjects = new java.util.ArrayList<>();
            this.internships = new java.util.ArrayList<>();
            this.hackathons = new java.util.ArrayList<>();
            this.volunteerWork = new java.util.ArrayList<>();
            this.academicAchievements = new java.util.ArrayList<>();
            this.courseWork = new java.util.ArrayList<>();
        }
        
        // Getters and setters
        public List<String> getAcademicProjects() { return academicProjects; }
        public void setAcademicProjects(List<String> academicProjects) { this.academicProjects = academicProjects; }
        
        public List<String> getInternships() { return internships; }
        public void setInternships(List<String> internships) { this.internships = internships; }
        
        public List<String> getHackathons() { return hackathons; }
        public void setHackathons(List<String> hackathons) { this.hackathons = hackathons; }
        
        public List<String> getVolunteerWork() { return volunteerWork; }
        public void setVolunteerWork(List<String> volunteerWork) { this.volunteerWork = volunteerWork; }
        
        public List<String> getAcademicAchievements() { return academicAchievements; }
        public void setAcademicAchievements(List<String> academicAchievements) { this.academicAchievements = academicAchievements; }
        
        public String getGithubProfile() { return githubProfile; }
        public void setGithubProfile(String githubProfile) { this.githubProfile = githubProfile; }
        
        public List<String> getCourseWork() { return courseWork; }
        public void setCourseWork(List<String> courseWork) { this.courseWork = courseWork; }
    }
}