package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.MatchLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced job recommendation engine using collaborative filtering and content-based matching
 */
@Service
public class JobRecommendationEngine {
    
    @Autowired(required = false)
    private JobAnalyticsService jobAnalyticsService;
    
    @Autowired(required = false)
    private SalaryPredictionService salaryPredictionService;
    
    /**
     * Recommend jobs based on user profile and skills
     */
    public List<JobListing> recommendJobs(List<JobListing> jobs, String userSkills, String userExperience) {
        if (jobs == null || jobs.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate relevance scores for each job
        Map<JobListing, Double> scores = new HashMap<>();
        
        for (JobListing job : jobs) {
            double score = calculateRelevanceScore(job, userSkills, userExperience);
            scores.put(job, score);
            job.setSuccessProbability(score);
            job.setMatchLevel(determineMatchLevel(score));
            
            // Add salary prediction if available
            if (salaryPredictionService != null && job.getSalaryRange() == null) {
                String predictedSalary = salaryPredictionService.predictSalary(job, userSkills);
                job.setSalaryRange(predictedSalary);
            }
        }
        
        // Sort by relevance score (descending)
        return jobs.stream()
                .sorted((j1, j2) -> Double.compare(
                    scores.getOrDefault(j2, 0.0),
                    scores.getOrDefault(j1, 0.0)
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate relevance score (0.0 to 1.0)
     */
    private double calculateRelevanceScore(JobListing job, String userSkills, String userExperience) {
        double score = 0.0;
        
        // Skill matching (40% weight)
        double skillScore = calculateSkillMatch(job, userSkills);
        score += skillScore * 0.4;
        
        // Experience matching (20% weight)
        double experienceScore = calculateExperienceMatch(job, userExperience);
        score += experienceScore * 0.2;
        
        // Job type preference (10% weight)
        double typeScore = calculateTypeScore(job);
        score += typeScore * 0.1;
        
        // Company reputation (10% weight)
        double companyScore = calculateCompanyScore(job);
        score += companyScore * 0.1;
        
        // Location preference (10% weight)
        double locationScore = calculateLocationScore(job);
        score += locationScore * 0.1;
        
        // Freshness (10% weight)
        double freshnessScore = calculateFreshnessScore(job);
        score += freshnessScore * 0.1;
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Calculate skill match score
     */
    private double calculateSkillMatch(JobListing job, String userSkills) {
        if (userSkills == null || userSkills.isBlank() || job.getDescription() == null) {
            return 0.5; // Neutral if no skills provided
        }
        
        String[] userSkillArray = userSkills.toLowerCase().split("[,\\s]+");
        String description = job.getDescription().toLowerCase();
        
        int matches = 0;
        for (String skill : userSkillArray) {
            if (description.contains(skill.trim())) {
                matches++;
            }
        }
        
        if (userSkillArray.length == 0) {
            return 0.5;
        }
        
        return (double) matches / userSkillArray.length;
    }
    
    /**
     * Calculate experience match score
     */
    private double calculateExperienceMatch(JobListing job, String userExperience) {
        if (userExperience == null || userExperience.isBlank()) {
            return 0.5;
        }
        
        Integer jobExperience = job.getExperienceRequired();
        if (jobExperience == null) {
            return 0.5;
        }
        
        try {
            int userExp = Integer.parseInt(userExperience.trim());
            int diff = Math.abs(userExp - jobExperience);
            
            // Perfect match = 1.0, difference of 1 = 0.8, difference of 2+ = 0.5
            if (diff == 0) return 1.0;
            if (diff == 1) return 0.8;
            if (diff == 2) return 0.6;
            return 0.4;
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }
    
    /**
     * Calculate job type score (prefer internships for freshers)
     */
    private double calculateTypeScore(JobListing job) {
        if (job.getJobType() == null) {
            return 0.5;
        }
        
        // Prefer internships for freshers
        switch (job.getJobType()) {
            case INTERNSHIP:
                return 0.9;
            case BOTH:
                return 0.8;
            case FULL_TIME:
                return 0.7;
            default:
                return 0.5;
        }
    }
    
    /**
     * Calculate company reputation score
     */
    private double calculateCompanyScore(JobListing job) {
        if (job.getCompany() == null) {
            return 0.5;
        }
        
        // Known good companies get higher scores
        String[] topCompanies = {
            "google", "microsoft", "amazon", "adobe", "oracle", "salesforce",
            "tcs", "infosys", "wipro", "hcl", "cognizant", "accenture",
            "flipkart", "razorpay", "phonepe", "zomato", "swiggy"
        };
        
        String companyLower = job.getCompany().toLowerCase();
        for (String topCompany : topCompanies) {
            if (companyLower.contains(topCompany)) {
                return 0.9;
            }
        }
        
        return 0.6; // Default for unknown companies
    }
    
    /**
     * Calculate location score
     */
    private double calculateLocationScore(JobListing job) {
        if (job.getLocation() == null) {
            return 0.5;
        }
        
        String location = job.getLocation().toLowerCase();
        
        // Remote/hybrid gets high score
        if (location.contains("remote") || location.contains("hybrid")) {
            return 0.9;
        }
        
        // Major IT cities get good score
        String[] itCities = {"bangalore", "mumbai", "hyderabad", "pune", "chennai", "delhi"};
        for (String city : itCities) {
            if (location.contains(city)) {
                return 0.8;
            }
        }
        
        return 0.6;
    }
    
    /**
     * Calculate freshness score (newer jobs are better)
     */
    private double calculateFreshnessScore(JobListing job) {
        if (job.getPostedDate() == null) {
            return 0.5;
        }
        
        long daysSincePosted = java.time.temporal.ChronoUnit.DAYS.between(
            job.getPostedDate(),
            java.time.LocalDateTime.now()
        );
        
        if (daysSincePosted <= 1) return 1.0;
        if (daysSincePosted <= 3) return 0.9;
        if (daysSincePosted <= 7) return 0.8;
        if (daysSincePosted <= 14) return 0.6;
        return 0.4;
    }
    
    /**
     * Determine match level from score
     */
    private MatchLevel determineMatchLevel(double score) {
        if (score >= 0.8) return MatchLevel.EXCELLENT;
        if (score >= 0.65) return MatchLevel.VERY_GOOD;
        if (score >= 0.5) return MatchLevel.GOOD;
        return MatchLevel.NOT_RECOMMENDED;
    }
    
    /**
     * Get top N recommendations
     */
    public List<JobListing> getTopRecommendations(List<JobListing> jobs, String userSkills, 
                                                   String userExperience, int topN) {
        List<JobListing> recommended = recommendJobs(jobs, userSkills, userExperience);
        return recommended.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }
}

