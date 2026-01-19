package com.resumeopt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a structured, ATS-friendly view of a resume.
 * Not a JPA entity – purely a view model for templates/PDF rendering.
 */
public class StructuredResumeView {

    // Header / contact
    public static class Header {
        private String fullName;
        private String email;
        private String phone;
        private String location;
        private String linkedin;
        private String website;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getLinkedin() { return linkedin; }
        public void setLinkedin(String linkedin) { this.linkedin = linkedin; }
        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }
    }

    public static class SkillGroup {
        private String groupName;
        private List<String> skills = new ArrayList<>();

        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }
    }

    public static class ExperienceItem {
        private String company;
        private String title;
        private String location;
        private String startDate;
        private String endDate;
        private List<String> bullets = new ArrayList<>();

        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public List<String> getBullets() { return bullets; }
        public void setBullets(List<String> bullets) { this.bullets = bullets; }
    }

    public static class ProjectItem {
        private String name;
        private String role;
        private String dates;
        private List<String> bullets = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getDates() { return dates; }
        public void setDates(String dates) { this.dates = dates; }
        public List<String> getBullets() { return bullets; }
        public void setBullets(List<String> bullets) { this.bullets = bullets; }
    }

    public static class EducationItem {
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private String dates;
        private List<String> bullets = new ArrayList<>();

        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }
        public String getDegree() { return degree; }
        public void setDegree(String degree) { this.degree = degree; }
        public String getFieldOfStudy() { return fieldOfStudy; }
        public void setFieldOfStudy(String fieldOfStudy) { this.fieldOfStudy = fieldOfStudy; }
        public String getDates() { return dates; }
        public void setDates(String dates) { this.dates = dates; }
        public List<String> getBullets() { return bullets; }
        public void setBullets(List<String> bullets) { this.bullets = bullets; }
    }

    public static class AchievementItem {
        private String title;
        private String issuer;
        private String date;
        private String details;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }

    private Header header = new Header();
    private String summary;
    private List<SkillGroup> skills = new ArrayList<>();
    private List<ExperienceItem> experience = new ArrayList<>();
    private List<ProjectItem> projects = new ArrayList<>();
    private List<EducationItem> education = new ArrayList<>();
    private List<AchievementItem> achievements = new ArrayList<>();

    /**
     * Catch-all for any content that could not be confidently structured.
     * Ensures we never lose original information.
     */
    private String otherContent;

    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<SkillGroup> getSkills() { return skills; }
    public void setSkills(List<SkillGroup> skills) { this.skills = skills; }
    public List<ExperienceItem> getExperience() { return experience; }
    public void setExperience(List<ExperienceItem> experience) { this.experience = experience; }
    public List<ProjectItem> getProjects() { return projects; }
    public void setProjects(List<ProjectItem> projects) { this.projects = projects; }
    public List<EducationItem> getEducation() { return education; }
    public void setEducation(List<EducationItem> education) { this.education = education; }
    public List<AchievementItem> getAchievements() { return achievements; }
    public void setAchievements(List<AchievementItem> achievements) { this.achievements = achievements; }
    public String getOtherContent() { return otherContent; }
    public void setOtherContent(String otherContent) { this.otherContent = otherContent; }
}


