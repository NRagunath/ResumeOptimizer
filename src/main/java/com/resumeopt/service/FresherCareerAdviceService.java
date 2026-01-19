package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FresherCareerAdviceService {
    
    /**
     * Provides fresher-specific career advice based on resume and job description
     */
    public FresherCareerAdviceResponse getFresherCareerAdvice(String resumeText, JobListing job, String academicBackground) {
        FresherCareerAdviceResponse advice = new FresherCareerAdviceResponse();
        
        // Generate personalized advice based on resume and job
        advice.setPersonalizedTips(generatePersonalizedTips(resumeText, job));
        
        // Generate interview preparation advice
        advice.setInterviewPrepTips(generateInterviewPrepTips(job));
        
        // Generate skill development recommendations
        advice.setSkillDevelopmentTips(generateSkillDevelopmentTips(resumeText, job));
        
        // Generate salary expectation guidance
        advice.setSalaryGuidance(generateSalaryGuidance(job, academicBackground));
        
        // Generate networking tips
        advice.setNetworkingTips(generateNetworkingTips(job));
        
        // Generate application strategy
        advice.setApplicationStrategy(generateApplicationStrategy(resumeText, job));
        
        return advice;
    }
    
    /**
     * Generates personalized tips based on resume and job requirements
     */
    private List<String> generatePersonalizedTips(String resumeText, JobListing job) {
        List<String> tips = new ArrayList<>();
        
        // Analyze resume against job requirements
        if (job != null && job.getDescription() != null) {
            String jobDesc = job.getDescription().toLowerCase();
            
            // Check for technical skills gaps
            if (jobDesc.contains("java") && !resumeText.toLowerCase().contains("java")) {
                tips.add("Consider learning Java as it's frequently required for this role");
            }
            
            if (jobDesc.contains("python") && !resumeText.toLowerCase().contains("python")) {
                tips.add("Python is mentioned in the job description - consider adding it to your skillset");
            }
            
            if (jobDesc.contains("react") && !resumeText.toLowerCase().contains("react")) {
                tips.add("React is a key requirement for this role - consider building a small project with it");
            }
            
            if (jobDesc.contains("sql") && !resumeText.toLowerCase().contains("sql")) {
                tips.add("Database skills (SQL) are important for this position - practice basic queries");
            }
        }
        
        // Add general fresher tips
        tips.add("Highlight your academic projects prominently in your resume");
        tips.add("Include any internships, even if they weren't in the exact field");
        tips.add("Mention relevant coursework that aligns with the job requirements");
        tips.add("Emphasize soft skills like communication, teamwork, and problem-solving");
        tips.add("Consider including GitHub profile or portfolio links if you have them");
        
        return tips;
    }
    
    /**
     * Generates interview preparation tips specific to the job
     */
    private List<String> generateInterviewPrepTips(JobListing job) {
        List<String> tips = new ArrayList<>();
        
        if (job != null) {
            String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
            String desc = job.getDescription() != null ? job.getDescription().toLowerCase() : "";
            
            // Add role-specific interview tips
            if (title.contains("software") || title.contains("developer") || desc.contains("coding")) {
                tips.add("Practice common data structure and algorithm problems");
                tips.add("Review your academic projects and be ready to discuss them in detail");
                tips.add("Prepare examples of how you solved technical problems");
                tips.add("Review the company's tech stack and be familiar with their tools");
            }
            
            if (title.contains("data") || desc.contains("data")) {
                tips.add("Review statistical concepts and data analysis techniques");
                tips.add("Be prepared to discuss data visualization tools you've used");
                tips.add("Practice explaining complex data insights in simple terms");
            }
            
            // Add general interview tips for freshers
            tips.add("Research the company thoroughly - their products, culture, and recent news");
            tips.add("Prepare answers for common fresher questions like 'Why should we hire you?'");
            tips.add("Practice explaining gaps in your experience positively");
            tips.add("Prepare questions to ask the interviewer about growth opportunities");
            tips.add("Dress professionally and maintain good body language");
        } else {
            // General interview tips for freshers
            tips.add("Research the company thoroughly - their products, culture, and recent news");
            tips.add("Prepare answers for common fresher questions like 'Why should we hire you?'");
            tips.add("Practice explaining gaps in your experience positively");
            tips.add("Prepare questions to ask the interviewer about growth opportunities");
            tips.add("Dress professionally and maintain good body language");
        }
        
        return tips;
    }
    
    /**
     * Generates skill development recommendations
     */
    private List<String> generateSkillDevelopmentTips(String resumeText, JobListing job) {
        List<String> tips = new ArrayList<>();
        
        if (job != null && job.getDescription() != null) {
            String jobDesc = job.getDescription().toLowerCase();
            
            // Identify missing skills and suggest learning paths
            if (jobDesc.contains("cloud") && !resumeText.toLowerCase().contains("cloud")) {
                tips.add("Learn cloud computing basics (AWS, Azure, or GCP) - start with free tier accounts");
            }
            
            if (jobDesc.contains("agile") && !resumeText.toLowerCase().contains("agile")) {
                tips.add("Familiarize yourself with Agile methodologies and Scrum practices");
            }
            
            if (jobDesc.contains("docker") && !resumeText.toLowerCase().contains("docker")) {
                tips.add("Learn Docker for containerization - it's increasingly important in modern development");
            }
            
            if (jobDesc.contains("git") && !resumeText.toLowerCase().contains("git")) {
                tips.add("Master Git version control - essential for any development role");
            }
        }
        
        // Add general skill development tips for freshers
        tips.add("Build a portfolio of projects that demonstrate your skills");
        tips.add("Contribute to open-source projects to gain real-world experience");
        tips.add("Take online courses on platforms like Coursera, edX, or Udemy");
        tips.add("Join professional communities and attend virtual meetups");
        tips.add("Consider earning relevant certifications to boost your profile");
        
        return tips;
    }
    
    /**
     * Generates salary expectation guidance
     */
    private String generateSalaryGuidance(JobListing job, String academicBackground) {
        StringBuilder guidance = new StringBuilder();
        
        guidance.append("Salary expectations for entry-level positions typically depend on location, company size, and your educational background. ");
        
        if (academicBackground != null) {
            if (academicBackground.toLowerCase().contains("computer science") || 
                academicBackground.toLowerCase().contains("engineering")) {
                guidance.append("With a technical degree, entry-level positions in software development typically range from ₹3-8 LPA in India, depending on the company and location. ");
            } else {
                guidance.append("For non-technical roles, entry-level salaries typically range from ₹2-5 LPA in India. ");
            }
        }
        
        guidance.append("Remember that initial salary is just one component - consider growth opportunities, learning potential, and benefits. ");
        guidance.append("As a fresher, focus on learning and growth rather than just the salary figure. ");
        guidance.append("Many companies offer performance-based increments and stock options that can significantly increase your total compensation over time.");
        
        return guidance.toString();
    }
    
    /**
     * Generates networking tips
     */
    private List<String> generateNetworkingTips(JobListing job) {
        List<String> tips = new ArrayList<>();
        
        tips.add("Connect with the hiring manager or team members on LinkedIn");
        tips.add("Join industry-specific groups and communities");
        tips.add("Attend virtual tech meetups and webinars");
        tips.add("Follow the company and its employees on social media");
        tips.add("Engage with their content by commenting thoughtfully");
        tips.add("Participate in online coding challenges and hackathons");
        tips.add("Consider attending career fairs and industry conferences");
        tips.add("Build relationships with seniors and alumni in your field");
        
        return tips;
    }
    
    /**
     * Generates application strategy
     */
    private List<String> generateApplicationStrategy(String resumeText, JobListing job) {
        List<String> strategy = new ArrayList<>();
        
        strategy.add("Tailor your cover letter to highlight how your academic background aligns with the role");
        strategy.add("Quantify your achievements in projects and internships wherever possible");
        strategy.add("Mention any relevant coursework or certifications that match job requirements");
        strategy.add("Highlight soft skills with specific examples from academic or extracurricular activities");
        strategy.add("Keep the resume concise (ideally one page) but comprehensive");
        
        if (job != null) {
            String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
            if (title.contains("developer") || title.contains("engineer")) {
                strategy.add("Include links to your GitHub profile or portfolio website");
                strategy.add("Mention any coding projects you've completed, even personal ones");
            }
        }
        
        strategy.add("Apply to similar roles to increase your chances while maintaining quality");
        strategy.add("Follow up appropriately after applying, but don't be too persistent");
        
        return strategy;
    }
    
    /**
     * Inner class to hold fresher career advice response
     */
    public static class FresherCareerAdviceResponse {
        private List<String> personalizedTips;
        private List<String> interviewPrepTips;
        private List<String> skillDevelopmentTips;
        private String salaryGuidance;
        private List<String> networkingTips;
        private List<String> applicationStrategy;
        
        public FresherCareerAdviceResponse() {
            this.personalizedTips = new ArrayList<>();
            this.interviewPrepTips = new ArrayList<>();
            this.skillDevelopmentTips = new ArrayList<>();
            this.networkingTips = new ArrayList<>();
            this.applicationStrategy = new ArrayList<>();
        }
        
        // Getters and setters
        public List<String> getPersonalizedTips() { return personalizedTips; }
        public void setPersonalizedTips(List<String> personalizedTips) { this.personalizedTips = personalizedTips; }
        
        public List<String> getInterviewPrepTips() { return interviewPrepTips; }
        public void setInterviewPrepTips(List<String> interviewPrepTips) { this.interviewPrepTips = interviewPrepTips; }
        
        public List<String> getSkillDevelopmentTips() { return skillDevelopmentTips; }
        public void setSkillDevelopmentTips(List<String> skillDevelopmentTips) { this.skillDevelopmentTips = skillDevelopmentTips; }
        
        public String getSalaryGuidance() { return salaryGuidance; }
        public void setSalaryGuidance(String salaryGuidance) { this.salaryGuidance = salaryGuidance; }
        
        public List<String> getNetworkingTips() { return networkingTips; }
        public void setNetworkingTips(List<String> networkingTips) { this.networkingTips = networkingTips; }
        
        public List<String> getApplicationStrategy() { return applicationStrategy; }
        public void setApplicationStrategy(List<String> applicationStrategy) { this.applicationStrategy = applicationStrategy; }
    }
}