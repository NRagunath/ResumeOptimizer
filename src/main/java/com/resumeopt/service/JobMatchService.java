package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.MatchLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for calculating and displaying job matches with user-friendly labels.
 */
@Service
public class JobMatchService {
    
    @Autowired(required = false)
    private ResumeOptimizationService optimizationService;
    
    /**
     * Calculate match level for a job listing based on resume text
     */
    public MatchLevel calculateMatchLevel(String resumeText, JobListing job) {
        if (resumeText == null || job == null || job.getDescription() == null) {
            return MatchLevel.NOT_RECOMMENDED;
        }
        
        // Use advanced ATS scoring if available
        if (optimizationService != null) {
            try {
                var result = optimizationService.optimize(resumeText, job.getDescription());
                double score = result.optimizedScore();
                return scoreToMatchLevel(score);
            } catch (Exception e) {
                System.err.println("Error calculating match level: " + e.getMessage());
            }
        }
        
        // Fallback to basic matching
        return calculateBasicMatchLevel(resumeText, job);
    }
    
    /**
     * Convert score to MatchLevel
     */
    private MatchLevel scoreToMatchLevel(double score) {
        if (score >= 0.90) return MatchLevel.EXCELLENT;
        if (score >= 0.75) return MatchLevel.VERY_GOOD;
        if (score >= 0.60) return MatchLevel.GOOD;
        return MatchLevel.NOT_RECOMMENDED;
    }
    
    /**
     * Basic match level calculation (fallback)
     */
    private MatchLevel calculateBasicMatchLevel(String resumeText, JobListing job) {
        Set<String> resumeTokens = normalize(resumeText);
        Set<String> jobTokens = normalize(job.getDescription() + " " + job.getTitle());
        
        long overlap = jobTokens.stream().filter(resumeTokens::contains).count();
        double ratio = jobTokens.isEmpty() ? 0.0 : (double) overlap / jobTokens.size();
        
        return scoreToMatchLevel(ratio);
    }
    
    /**
     * Get user-friendly label for match level
     */
    public String getMatchLabel(MatchLevel level) {
        if (level == null) {
            return "Unknown";
        }
        
        switch (level) {
            case EXCELLENT:
                return "Get the Job";
            case VERY_GOOD:
                return "Strong Match";
            case GOOD:
                return "Good Match";
            case NOT_RECOMMENDED:
                return "Do Not Apply";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Get CSS class for match level badge
     */
    public String getMatchBadgeClass(MatchLevel level) {
        if (level == null) {
            return "badge bg-secondary";
        }
        
        switch (level) {
            case EXCELLENT:
                return "badge bg-success";
            case VERY_GOOD:
                return "badge bg-primary";
            case GOOD:
                return "badge bg-warning text-dark";
            case NOT_RECOMMENDED:
                return "badge bg-danger";
            default:
                return "badge bg-secondary";
        }
    }
    
    /**
     * Get recommendation text for match level
     */
    public String getMatchRecommendation(MatchLevel level) {
        if (level == null) {
            return "Unable to determine match quality.";
        }
        
        switch (level) {
            case EXCELLENT:
                return "Excellent match! Your resume aligns perfectly with this position. Apply with confidence.";
            case VERY_GOOD:
                return "Strong match! Your skills and experience closely match the requirements. Highly recommended to apply.";
            case GOOD:
                return "Good match. Your resume has relevant qualifications, but consider tailoring it further for better results.";
            case NOT_RECOMMENDED:
                return "Low match. This position may not be the best fit. Consider focusing on roles that better match your skills.";
            default:
                return "Unable to determine match quality.";
        }
    }
    
    /**
     * Get match explanation/reasoning
     */
    public List<String> getMatchExplanation(String resumeText, JobListing job) {
        List<String> explanations = new ArrayList<>();
        
        if (resumeText == null || job == null || job.getDescription() == null) {
            explanations.add("Unable to analyze match - missing information.");
            return explanations;
        }
        
        Set<String> resumeTokens = normalize(resumeText);
        Set<String> jobTokens = normalize(job.getDescription() + " " + job.getTitle());
        
        // Find matching skills
        Set<String> matchingSkills = new HashSet<>(jobTokens);
        matchingSkills.retainAll(resumeTokens);
        
        if (!matchingSkills.isEmpty()) {
            explanations.add("Matching skills: " + String.join(", ", matchingSkills.stream().limit(10).toList()));
        }
        
        // Find missing skills
        Set<String> missingSkills = new HashSet<>(jobTokens);
        missingSkills.removeAll(resumeTokens);
        
        if (!missingSkills.isEmpty()) {
            explanations.add("Missing skills: " + String.join(", ", missingSkills.stream().limit(10).toList()));
        }
        
        // Calculate match percentage
        double matchPercentage = jobTokens.isEmpty() ? 0.0 : 
            ((double) matchingSkills.size() / jobTokens.size()) * 100;
        explanations.add(String.format("Match percentage: %.1f%%", matchPercentage));
        
        return explanations;
    }
    
    /**
     * Normalize text for comparison
     */
    private Set<String> normalize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        
        return new HashSet<>(Arrays.asList(
            text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+")
        ));
    }
    
    /**
     * Calculate match score percentage
     */
    public double calculateMatchScore(String resumeText, JobListing job) {
        if (resumeText == null || job == null || job.getDescription() == null) {
            return 0.0;
        }
        
        Set<String> resumeTokens = normalize(resumeText);
        Set<String> jobTokens = normalize(job.getDescription() + " " + job.getTitle());
        
        if (jobTokens.isEmpty()) {
            return 0.0;
        }
        
        long overlap = jobTokens.stream().filter(resumeTokens::contains).count();
        return ((double) overlap / jobTokens.size()) * 100.0;
    }
}

