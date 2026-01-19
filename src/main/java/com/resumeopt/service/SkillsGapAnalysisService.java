package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.SkillsGapAnalysisResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillsGapAnalysisService {

    /**
     * Analyzes a fresher's resume against a job description to identify missing keywords/skills
     */
    public SkillsGapAnalysisResult analyzeSkillsGap(String resumeText, JobListing job) {
        // Extract skills from resume
        Set<String> resumeSkills = extractSkillsFromResume(resumeText);
        
        // Extract required skills from job description
        Set<String> requiredSkills = extractSkillsFromJobDescription(job);
        
        // Find missing skills
        Set<String> missingSkills = new HashSet<>(requiredSkills);
        missingSkills.removeAll(resumeSkills);
        
        // Find matching skills
        Set<String> matchingSkills = new HashSet<>(resumeSkills);
        matchingSkills.retainAll(requiredSkills);
        
        // Calculate match percentage
        double matchPercentage = requiredSkills.isEmpty() ? 0 : 
            (double) matchingSkills.size() / requiredSkills.size() * 100;
        
        List<String> recommendations = recommendSkillsForGap(missingSkills);
        List<String> certificationSuggestions = suggestCertifications(missingSkills);
        
        return new com.resumeopt.model.SkillsGapAnalysisResult(
            missingSkills,
            matchingSkills,
            recommendations,
            certificationSuggestions
        );
    }
    
    /**
     * Extracts skills from resume text using keyword matching
     */
    private Set<String> extractSkillsFromResume(String resumeText) {
        if (resumeText == null || resumeText.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> skills = new HashSet<>();
        String lowerText = resumeText.toLowerCase();
        
        // Define common skill keywords
        String[] skillKeywords = {
            "java", "python", "javascript", "react", "angular", "node", "spring", "sql",
            "html", "css", "mongodb", "mysql", "postgresql", "git", "docker", "kubernetes",
            "aws", "azure", "gcp", "linux", "c++", "c#", "php", "ruby", "typescript",
            "vue", "express", "django", "flask", "tensorflow", "pytorch", "machine learning",
            "data science", "ai", "artificial intelligence", "deep learning", "android",
            "ios", "mobile development", "api", "rest", "microservices", "agile", "scrum",
            "project management", "ui/ux", "design", "testing", "ci/cd", "jenkins",
            "sql server", "oracle", "firebase", "spring boot", "hibernate", "jpa",
            "bootstrap", "sass", "less", "webpack", "babel", "npm", "yarn", "redux",
            "graphql", "postman", "junit", "selenium", "cucumber", "maven", "gradle",
            "jenkins", "gitlab", "github", "bitbucket", "bash", "powershell", "scala",
            "go", "rust", "swift", "kotlin", "flutter", "react native", "xamarin",
            "blockchain", "ethereum", "solidity", "web3", "ethereum", "solidity",
            "data structures", "algorithms", "problem solving", "communication",
            "teamwork", "leadership", "adaptability", "critical thinking", "creativity",
            "time management", "organization", "analytical", "research", "presentation",
            "negotiation", "conflict resolution", "emotional intelligence"
        };
        
        for (String skill : skillKeywords) {
            if (lowerText.contains(skill.toLowerCase())) {
                skills.add(skill);
            }
        }
        
        return skills;
    }
    
    /**
     * Suggests relevant certifications based on missing skills
     */
    private List<String> suggestCertifications(Set<String> missingSkills) {
        List<String> certifications = new ArrayList<>();
        
        for (String skill : missingSkills) {
            switch (skill.toLowerCase()) {
                case "java":
                    certifications.add("Oracle Certified Associate, Java SE Programmer");
                    break;
                case "python":
                    certifications.add("Python Institute Certified Python Programmer");
                    break;
                case "aws":
                    certifications.add("AWS Certified Solutions Architect - Associate");
                    break;
                case "azure":
                    certifications.add("Microsoft Azure Fundamentals (AZ-900)");
                    break;
                case "docker":
                    certifications.add("Docker Certified Associate");
                    break;
                case "kubernetes":
                    certifications.add("Certified Kubernetes Administrator (CKA)");
                    break;
                case "react":
                    certifications.add("Frontend Web Development with React Specialization");
                    break;
                case "spring":
                    certifications.add("Spring Professional Certification");
                    break;
                case "sql":
                    certifications.add("Database Management Essentials");
                    break;
                case "machine learning":
                    certifications.add("Machine Learning by Stanford University (Coursera)");
                    break;
                case "data science":
                    certifications.add("Data Science Specialization by Johns Hopkins University");
                    break;
                default:
                    // Add generic certification suggestions for other skills
                    certifications.add("Consider relevant certification in " + skill + " from recognized platforms");
                    break;
            }
        }
        
        return certifications;
    }
    
    /**
     * Extracts required skills from job description
     */
    private Set<String> extractSkillsFromJobDescription(JobListing job) {
        if (job == null || (job.getDescription() == null && job.getTitle() == null)) {
            return new HashSet<>();
        }
        
        StringBuilder combinedText = new StringBuilder();
        if (job.getTitle() != null) {
            combinedText.append(job.getTitle()).append(" ");
        }
        if (job.getDescription() != null) {
            combinedText.append(job.getDescription());
        }
        
        return extractSkillsFromResume(combinedText.toString());
    }
    
    /**
     * Recommends skills to acquire based on the missing skills
     */
    private List<String> recommendSkillsForGap(Set<String> missingSkills) {
        List<String> recommendations = new ArrayList<>();
        
        // Group missing skills by category and prioritize
        Map<String, List<String>> skillCategories = groupSkillsByCategory(missingSkills);
        
        for (Map.Entry<String, List<String>> entry : skillCategories.entrySet()) {
            String category = entry.getKey();
            List<String> skillsInCategory = entry.getValue();
            
            // Recommend top 3 skills from each category
            recommendations.addAll(skillsInCategory.stream()
                .limit(3)
                .collect(Collectors.toList()));
        }
        
        return recommendations;
    }
    
    /**
     * Groups skills by category for better recommendations
     */
    private Map<String, List<String>> groupSkillsByCategory(Set<String> skills) {
        Map<String, List<String>> categories = new HashMap<>();
        
        // Programming Languages
        List<String> programmingLanguages = skills.stream()
            .filter(skill -> Arrays.asList(
                "java", "python", "javascript", "c++", "c#", "php", "ruby", "go", "rust", "kotlin", "swift"
            ).contains(skill.toLowerCase()))
            .collect(Collectors.toList());
        
        // Frameworks & Libraries
        List<String> frameworks = skills.stream()
            .filter(skill -> Arrays.asList(
                "react", "angular", "vue", "spring", "django", "flask", "express", "bootstrap", "jquery"
            ).contains(skill.toLowerCase()))
            .collect(Collectors.toList());
        
        // Databases
        List<String> databases = skills.stream()
            .filter(skill -> Arrays.asList(
                "sql", "mysql", "postgresql", "mongodb", "oracle", "redis", "cassandra"
            ).contains(skill.toLowerCase()))
            .collect(Collectors.toList());
        
        // Cloud & DevOps
        List<String> cloudDevOps = skills.stream()
            .filter(skill -> Arrays.asList(
                "aws", "azure", "gcp", "docker", "kubernetes", "jenkins", "git", "ci/cd"
            ).contains(skill.toLowerCase()))
            .collect(Collectors.toList());
        
        // Soft Skills
        List<String> softSkills = skills.stream()
            .filter(skill -> Arrays.asList(
                "communication", "teamwork", "leadership", "problem solving", "critical thinking"
            ).contains(skill.toLowerCase()))
            .collect(Collectors.toList());
        
        if (!programmingLanguages.isEmpty()) {
            categories.put("Programming Languages", programmingLanguages);
        }
        if (!frameworks.isEmpty()) {
            categories.put("Frameworks & Libraries", frameworks);
        }
        if (!databases.isEmpty()) {
            categories.put("Databases", databases);
        }
        if (!cloudDevOps.isEmpty()) {
            categories.put("Cloud & DevOps", cloudDevOps);
        }
        if (!softSkills.isEmpty()) {
            categories.put("Soft Skills", softSkills);
        }
        
        // Add any remaining skills under 'Other'
        Set<String> allCategorizedSkills = new HashSet<>();
        categories.values().forEach(allCategorizedSkills::addAll);
        List<String> otherSkills = skills.stream()
            .filter(skill -> !allCategorizedSkills.contains(skill))
            .collect(Collectors.toList());
        
        if (!otherSkills.isEmpty()) {
            categories.put("Other", otherSkills);
        }
        
        return categories;
    }

}