package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.MatchLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobRecommendationService {
    private final JobAnalyticsService analytics;
    
    @Autowired(required = false)
    private JobMatchService jobMatchService;
    
    @Autowired(required = false)
    private ResumeOptimizationService optimizationService;

    public JobRecommendationService(JobAnalyticsService analytics) {
        this.analytics = analytics;
    }

    @io.micrometer.core.annotation.Timed(value = "jobs.recommend", description = "Recommendation computation time")
    public List<JobListing> recommend(String resumeText, List<JobListing> listings) {
        for (JobListing jl : listings) {
            // Use enhanced matching if available
            if (jobMatchService != null) {
                MatchLevel matchLevel = jobMatchService.calculateMatchLevel(resumeText, jl);
                jl.setMatchLevel(matchLevel);
                
                // Calculate match score percentage
                double matchScore = jobMatchService.calculateMatchScore(resumeText, jl);
                jl.setSuccessProbability(matchScore / 100.0);
            } else {
                // Fallback to basic matching
                Set<String> resumeTokens = normalize(resumeText);
                Set<String> jobTokens = normalize(jl.getDescription() + " " + jl.getTitle());
                int overlap = (int) jobTokens.stream().filter(resumeTokens::contains).count();
                double ratio = jobTokens.isEmpty() ? 0.0 : overlap * 1.0 / jobTokens.size();
                jl.setMatchLevel(scoreToLevel(ratio));
                jl.setSuccessProbability(ratio);
            }

            // Enrich with analytics for entry-level optimization
            analytics.enrichFromDescription(jl);
            
            // Use existing probability if not set by match service
            if (jl.getSuccessProbability() == null) {
                double prob = analytics.estimateSuccessProbability(resumeText, jl);
                jl.setSuccessProbability(prob);
            }
            
            List<String> gaps = analytics.findSkillGaps(resumeText, jl);
            jl.setSkillGaps(gaps);
            jl.setGuidanceTips(analytics.guidanceTips(gaps));
        }
        
        // Sort by match level (EXCELLENT first) then by success probability
        return listings.stream()
                .sorted(Comparator
                    .comparing(JobListing::getMatchLevel, Comparator.reverseOrder())
                    .thenComparing((JobListing jl) -> jl.getSuccessProbability() != null ? jl.getSuccessProbability() : 0.0, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get match explanation for a job listing
     */
    public Map<String, Object> getMatchDetails(String resumeText, JobListing job) {
        Map<String, Object> details = new HashMap<>();
        
        if (jobMatchService != null) {
            details.put("matchLevel", job.getMatchLevel());
            details.put("matchLabel", jobMatchService.getMatchLabel(job.getMatchLevel()));
            details.put("matchBadgeClass", jobMatchService.getMatchBadgeClass(job.getMatchLevel()));
            details.put("recommendation", jobMatchService.getMatchRecommendation(job.getMatchLevel()));
            details.put("explanation", jobMatchService.getMatchExplanation(resumeText, job));
            details.put("matchScore", jobMatchService.calculateMatchScore(resumeText, job));
        } else {
            details.put("matchLevel", job.getMatchLevel());
            details.put("matchLabel", getMatchLabel(job.getMatchLevel()));
            details.put("matchScore", job.getSuccessProbability() != null ? job.getSuccessProbability() * 100 : 0.0);
        }
        
        return details;
    }
    
    private String getMatchLabel(MatchLevel level) {
        if (level == null) return "Unknown";
        switch (level) {
            case EXCELLENT: return "Get the Job";
            case VERY_GOOD: return "Strong Match";
            case GOOD: return "Good Match";
            case NOT_RECOMMENDED: return "Do Not Apply";
            default: return "Unknown";
        }
    }

    public MatchLevel scoreToLevel(double ratio) {
        if (ratio >= 0.35) return MatchLevel.EXCELLENT;
        if (ratio >= 0.25) return MatchLevel.VERY_GOOD;
        if (ratio >= 0.15) return MatchLevel.GOOD;
        return MatchLevel.NOT_RECOMMENDED;
    }

    private Set<String> normalize(String text) {
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9\n ]", " ").split("\\s+"))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}