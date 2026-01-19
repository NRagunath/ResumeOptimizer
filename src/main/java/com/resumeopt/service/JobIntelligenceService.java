package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.MatchLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive job intelligence service that integrates all advanced features
 */
@Service
public class JobIntelligenceService {
    
    @Autowired(required = false)
    private JobRecommendationEngine recommendationEngine;
    
    @Autowired(required = false)
    private SalaryPredictionService salaryPredictionService;
    
    @Autowired(required = false)
    private CompanyReputationService companyReputationService;
    
    @Autowired(required = false)
    private ResumeMatchingService resumeMatchingService;
    
    @Autowired(required = false)
    private AdvancedJobTypeClassificationService jobTypeClassificationService;
    
    @Autowired(required = false)
    private AdvancedDataExtractionService dataExtractionService;
    
    /**
     * Comprehensive job analysis with all intelligence features
     */
    public JobIntelligence analyzeJob(JobListing job, String userSkills, String userExperience, String resumeText) {
        JobIntelligence intelligence = new JobIntelligence();
        intelligence.job = job;
        
        // 1. Job Type Classification
        if (jobTypeClassificationService != null) {
            intelligence.jobType = jobTypeClassificationService.classifyJob(job);
            intelligence.classificationConfidence = jobTypeClassificationService.getClassificationConfidence(job);
        }
        
        // 2. Data Extraction
        if (dataExtractionService != null) {
            dataExtractionService.enrichJobListing(job);
        }
        
        // 3. Salary Prediction
        if (salaryPredictionService != null) {
            intelligence.predictedSalary = salaryPredictionService.predictSalary(job, userSkills);
            if (job.getSalaryRange() == null) {
                job.setSalaryRange(intelligence.predictedSalary);
            }
        }
        
        // 4. Company Reputation
        if (companyReputationService != null && job.getCompany() != null) {
            intelligence.companyReputation = companyReputationService.getReputationScore(job.getCompany());
            intelligence.companyTier = companyReputationService.getCompanyTier(job.getCompany());
            intelligence.companyInsights = companyReputationService.getCompanyInsights(job.getCompany());
        }
        
        // 5. Resume Matching
        if (resumeMatchingService != null && resumeText != null && !resumeText.isBlank()) {
            intelligence.matchScore = resumeMatchingService.calculateMatchScore(resumeText, job);
            intelligence.skillGaps = resumeMatchingService.getSkillGaps(resumeText, job);
            intelligence.matchLevel = determineMatchLevel(intelligence.matchScore);
        }
        
        // 6. Recommendation Score
        if (recommendationEngine != null && userSkills != null) {
            List<JobListing> singleJob = Collections.singletonList(job);
            recommendationEngine.recommendJobs(singleJob, userSkills, userExperience);
            intelligence.recommendationScore = job.getSuccessProbability() != null ? 
                job.getSuccessProbability() : 0.5;
        }
        
        // 7. Overall Score Calculation
        intelligence.overallScore = calculateOverallScore(intelligence);
        
        // 8. Recommendations
        intelligence.recommendations = generateRecommendations(intelligence);
        
        return intelligence;
    }
    
    /**
     * Calculate overall score from all factors
     */
    private double calculateOverallScore(JobIntelligence intelligence) {
        double score = 0.0;
        double weight = 0.0;
        
        // Match score (40%)
        if (intelligence.matchScore > 0) {
            score += intelligence.matchScore * 0.4;
            weight += 0.4;
        }
        
        // Recommendation score (30%)
        if (intelligence.recommendationScore > 0) {
            score += intelligence.recommendationScore * 0.3;
            weight += 0.3;
        }
        
        // Company reputation (20%)
        if (intelligence.companyReputation > 0) {
            score += intelligence.companyReputation * 0.2;
            weight += 0.2;
        }
        
        // Classification confidence (10%)
        if (intelligence.classificationConfidence > 0) {
            score += intelligence.classificationConfidence * 0.1;
            weight += 0.1;
        }
        
        return weight > 0 ? score / weight : 0.5;
    }
    
    /**
     * Generate personalized recommendations
     */
    private List<String> generateRecommendations(JobIntelligence intelligence) {
        List<String> recommendations = new ArrayList<>();
        
        if (intelligence.matchScore < 0.5) {
            recommendations.add("Your resume needs improvement to match this job better");
        }
        
        if (intelligence.skillGaps != null && !intelligence.skillGaps.isEmpty()) {
            recommendations.add("Consider learning: " + String.join(", ", intelligence.skillGaps));
        }
        
        if (intelligence.companyReputation < 0.7) {
            recommendations.add("Research this company thoroughly before applying");
        }
        
        if (intelligence.predictedSalary != null && !intelligence.predictedSalary.contains("Not available")) {
            recommendations.add("Expected salary range: " + intelligence.predictedSalary);
        }
        
        if (intelligence.overallScore >= 0.8) {
            recommendations.add("This is an excellent match! Apply with confidence");
        } else if (intelligence.overallScore >= 0.6) {
            recommendations.add("Good match. Tailor your resume to improve chances");
        } else {
            recommendations.add("Consider improving your skills before applying");
        }
        
        return recommendations;
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
     * Get intelligence summary for multiple jobs
     */
    public Map<String, Object> getIntelligenceSummary(List<JobListing> jobs, String userSkills) {
        Map<String, Object> summary = new HashMap<>();
        
        if (jobs == null || jobs.isEmpty()) {
            return summary;
        }
        
        // Calculate average scores
        double avgMatchScore = jobs.stream()
                .filter(j -> j.getSuccessProbability() != null)
                .mapToDouble(JobListing::getSuccessProbability)
                .average()
                .orElse(0.0);
        
        summary.put("totalJobs", jobs.size());
        summary.put("averageMatchScore", avgMatchScore);
        
        // Count by match level
        Map<String, Long> matchLevelCounts = jobs.stream()
                .filter(j -> j.getMatchLevel() != null)
                .collect(Collectors.groupingBy(
                    j -> j.getMatchLevel().name(),
                    Collectors.counting()
                ));
        summary.put("matchLevelDistribution", matchLevelCounts);
        
        // Top recommended jobs
        List<JobListing> topJobs = jobs.stream()
                .filter(j -> j.getSuccessProbability() != null)
                .sorted((j1, j2) -> Double.compare(
                    j2.getSuccessProbability(),
                    j1.getSuccessProbability()
                ))
                .limit(5)
                .collect(Collectors.toList());
        summary.put("topRecommendedJobs", topJobs);
        
        return summary;
    }
    
    /**
     * Job intelligence data class
     */
    public static class JobIntelligence {
        public JobListing job;
        public com.resumeopt.model.JobType jobType;
        public double classificationConfidence = 0.0;
        public String predictedSalary;
        public double companyReputation = 0.0;
        public String companyTier;
        public Map<String, Object> companyInsights;
        public double matchScore = 0.0;
        public double recommendationScore = 0.0;
        public double overallScore = 0.0;
        public MatchLevel matchLevel;
        public List<String> skillGaps = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();
    }
}

