package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced resume matching service using semantic analysis
 */
@Service
public class ResumeMatchingService {
    
    /**
     * Calculate comprehensive match score between resume and job
     */
    public double calculateMatchScore(String resumeText, JobListing job) {
        if (resumeText == null || job == null) {
            return 0.0;
        }
        
        String jobDescription = job.getDescription() != null ? job.getDescription() : "";
        String jobTitle = job.getTitle() != null ? job.getTitle() : "";
        String combinedJobText = jobTitle + " " + jobDescription;
        
        // Extract features from resume
        ResumeFeatures resumeFeatures = extractResumeFeatures(resumeText);
        
        // Extract features from job
        JobFeatures jobFeatures = extractJobFeatures(combinedJobText);
        
        // Calculate match scores for different aspects
        double skillMatch = calculateSkillMatch(resumeFeatures.skills, jobFeatures.requiredSkills);
        double experienceMatch = calculateExperienceMatch(resumeFeatures.experience, jobFeatures.experienceRequired);
        double educationMatch = calculateEducationMatch(resumeFeatures.education, jobFeatures.educationRequired);
        double projectMatch = calculateProjectMatch(resumeFeatures.hasProjects, jobFeatures.technologies);
        
        // Weighted combination
        double totalScore = (skillMatch * 0.4) + 
                           (experienceMatch * 0.25) + 
                           (educationMatch * 0.15) + 
                           (projectMatch * 0.2);
        
        return Math.min(totalScore, 1.0);
    }
    
    /**
     * Extract features from resume text
     */
    private ResumeFeatures extractResumeFeatures(String resumeText) {
        ResumeFeatures features = new ResumeFeatures();
        String lowerText = resumeText.toLowerCase();
        
        // Extract skills
        String[] commonSkills = {
            "java", "python", "javascript", "react", "angular", "node.js", "spring",
            "sql", "mongodb", "aws", "docker", "kubernetes", "git", "agile",
            "machine learning", "data science", "devops", "microservices", "rest api"
        };
        
        for (String skill : commonSkills) {
            if (lowerText.contains(skill)) {
                features.skills.add(skill);
            }
        }
        
        // Extract experience (look for years)
        java.util.regex.Pattern expPattern = java.util.regex.Pattern.compile(
            "(\\d+)\\s*(?:years?|yrs?|months?)", java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = expPattern.matcher(resumeText);
        if (matcher.find()) {
            try {
                features.experience = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                features.experience = 0;
            }
        }
        
        // Extract education keywords
        if (lowerText.contains("bachelor") || lowerText.contains("b.tech") || 
            lowerText.contains("b.e") || lowerText.contains("bsc")) {
            features.education.add("bachelor");
        }
        if (lowerText.contains("master") || lowerText.contains("m.tech") || 
            lowerText.contains("m.e") || lowerText.contains("msc")) {
            features.education.add("master");
        }
        
        // Extract project keywords
        if (lowerText.contains("project") || lowerText.contains("developed") || 
            lowerText.contains("built") || lowerText.contains("created")) {
            features.hasProjects = true;
        }
        
        return features;
    }
    
    /**
     * Extract features from job description
     */
    private JobFeatures extractJobFeatures(String jobText) {
        JobFeatures features = new JobFeatures();
        String lowerText = jobText.toLowerCase();
        
        // Extract required skills
        String[] commonSkills = {
            "java", "python", "javascript", "react", "angular", "node.js", "spring",
            "sql", "mongodb", "aws", "docker", "kubernetes", "git", "agile",
            "machine learning", "data science", "devops", "microservices", "rest api"
        };
        
        for (String skill : commonSkills) {
            if (lowerText.contains(skill)) {
                features.requiredSkills.add(skill);
            }
        }
        
        // Extract experience requirement
        java.util.regex.Pattern expPattern = java.util.regex.Pattern.compile(
            "(\\d+)\\s*(?:to|-|–)?\\s*(\\d+)?\\s*(?:years?|yrs?)", java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = expPattern.matcher(jobText);
        if (matcher.find()) {
            try {
                features.experienceRequired = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                features.experienceRequired = 0;
            }
        }
        
        // Extract education requirements
        if (lowerText.contains("bachelor") || lowerText.contains("b.tech") || 
            lowerText.contains("b.e") || lowerText.contains("degree")) {
            features.educationRequired.add("bachelor");
        }
        if (lowerText.contains("master") || lowerText.contains("m.tech")) {
            features.educationRequired.add("master");
        }
        
        // Extract technologies
        features.technologies.addAll(features.requiredSkills);
        
        return features;
    }
    
    /**
     * Calculate skill match score
     */
    private double calculateSkillMatch(List<String> resumeSkills, List<String> jobSkills) {
        if (jobSkills.isEmpty()) {
            return 0.5; // Neutral if no skills specified
        }
        
        if (resumeSkills.isEmpty()) {
            return 0.0;
        }
        
        long matches = resumeSkills.stream()
                .filter(jobSkills::contains)
                .count();
        
        return (double) matches / jobSkills.size();
    }
    
    /**
     * Calculate experience match score
     */
    private double calculateExperienceMatch(int resumeExp, int jobExp) {
        if (jobExp == 0) {
            return 1.0; // No requirement
        }
        
        int diff = Math.abs(resumeExp - jobExp);
        if (diff == 0) return 1.0;
        if (diff == 1) return 0.8;
        if (diff == 2) return 0.6;
        return 0.4;
    }
    
    /**
     * Calculate education match score
     */
    private double calculateEducationMatch(List<String> resumeEdu, List<String> jobEdu) {
        if (jobEdu.isEmpty()) {
            return 0.5; // No requirement
        }
        
        boolean hasMatch = resumeEdu.stream().anyMatch(jobEdu::contains);
        return hasMatch ? 1.0 : 0.3;
    }
    
    /**
     * Calculate project match score
     */
    private double calculateProjectMatch(boolean hasProjects, List<String> technologies) {
        if (!hasProjects) {
            return 0.3;
        }
        
        // Having projects is good, especially if they use relevant technologies
        return 0.8;
    }
    
    /**
     * Get skill gaps between resume and job
     */
    public List<String> getSkillGaps(String resumeText, JobListing job) {
        if (resumeText == null || job == null || job.getDescription() == null) {
            return new ArrayList<>();
        }
        
        ResumeFeatures resumeFeatures = extractResumeFeatures(resumeText);
        JobFeatures jobFeatures = extractJobFeatures(job.getDescription());
        
        return jobFeatures.requiredSkills.stream()
                .filter(skill -> !resumeFeatures.skills.contains(skill))
                .collect(Collectors.toList());
    }
    
    /**
     * Resume features
     */
    private static class ResumeFeatures {
        List<String> skills = new ArrayList<>();
        int experience = 0;
        List<String> education = new ArrayList<>();
        boolean hasProjects = false;
    }
    
    /**
     * Job features
     */
    private static class JobFeatures {
        List<String> requiredSkills = new ArrayList<>();
        int experienceRequired = 0;
        List<String> educationRequired = new ArrayList<>();
        List<String> technologies = new ArrayList<>();
    }
}

