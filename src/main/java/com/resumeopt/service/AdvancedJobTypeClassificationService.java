package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.JobType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Advanced job type classification using weighted scoring, pattern matching, and context analysis
 */
@Service
public class AdvancedJobTypeClassificationService {
    
    // Weighted keywords with confidence scores
    private static final Map<String, Double> INTERNSHIP_KEYWORDS = createInternshipKeywords();
    private static final Map<String, Double> FULL_TIME_KEYWORDS = createFullTimeKeywords();
    
    private static Map<String, Double> createInternshipKeywords() {
        Map<String, Double> map = new HashMap<>();
        map.put("intern", 0.95);
        map.put("internship", 1.0);
        map.put("trainee", 0.85);
        map.put("traineeship", 0.9);
        map.put("apprentice", 0.8);
        map.put("student", 0.7);
        map.put("co-op", 0.9);
        map.put("coop", 0.9);
        map.put("summer intern", 0.95);
        map.put("winter intern", 0.95);
        map.put("graduate intern", 0.9);

        map.put("internship program", 1.0);
        map.put("stipend", 0.7);
        map.put("learn and earn", 0.8);
        map.put("training program", 0.75);
        map.put("entry level intern", 0.9);
        return Collections.unmodifiableMap(map);
    }
    
    private static Map<String, Double> createFullTimeKeywords() {
        Map<String, Double> map = new HashMap<>();
        map.put("full time", 0.95);
        map.put("full-time", 0.95);
        map.put("permanent", 0.9);
        map.put("regular", 0.85);
        map.put("fulltime", 0.95);
        map.put("ft", 0.8);
        map.put("f/t", 0.8);
        map.put("employee", 0.75);
        map.put("staff", 0.75);
        map.put("full time position", 0.95);
        map.put("permanent role", 0.9);
        map.put("regular employment", 0.85);
        map.put("benefits package", 0.7);
        map.put("health insurance", 0.65);
        map.put("pto", 0.6);
        map.put("paid time off", 0.6);
        return Collections.unmodifiableMap(map);
    }
    
    // Negative indicators (reduce confidence)
    private static final List<String> NEGATIVE_INTERNSHIP_INDICATORS = Arrays.asList(
        "senior", "lead", "manager", "director", "vp", "vice president", "5+ years", "10+ years"
    );
    
    private static final List<String> NEGATIVE_FULL_TIME_INDICATORS = Arrays.asList(
        "temporary", "contract", "freelance", "part-time", "part time", "hourly", "gig"
    );
    
    // Pattern matching for duration indicators
    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(\\d+)\\s*(months?|weeks?|days?)\\s*(intern|internship|program|duration)", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern STIPEND_PATTERN = Pattern.compile(
        "stipend|allowance|monthly.*\\d+.*rupees?|\\d+.*per month", Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Advanced classification using weighted scoring and context analysis
     */
    public JobType classifyJob(JobListing job) {
        if (job == null) {
            return JobType.FULL_TIME;
        }
        
        String title = normalizeText(job.getTitle());
        String description = normalizeText(job.getDescription());
        String combined = title + " " + description;
        
        // Calculate weighted scores
        double internshipScore = calculateInternshipScore(combined, title, description);
        double fullTimeScore = calculateFullTimeScore(combined, title, description);
        
        // Apply negative indicators
        internshipScore = applyNegativeIndicators(combined, internshipScore, NEGATIVE_INTERNSHIP_INDICATORS);
        fullTimeScore = applyNegativeIndicators(combined, fullTimeScore, NEGATIVE_FULL_TIME_INDICATORS);
        
        // Check for duration patterns (strong indicator of internship)
        if (DURATION_PATTERN.matcher(combined).find()) {
            internshipScore += 0.3;
        }
        
        // Check for stipend patterns
        if (STIPEND_PATTERN.matcher(combined).find()) {
            internshipScore += 0.2;
        }
        
        // Decision logic with thresholds
        double threshold = 0.4;
        boolean isInternship = internshipScore >= threshold;
        boolean isFullTime = fullTimeScore >= threshold;
        
        if (isInternship && isFullTime) {
            return JobType.BOTH;
        } else if (isInternship) {
            return JobType.INTERNSHIP;
        } else if (isFullTime) {
            return JobType.FULL_TIME;
        } else {
            // Default based on title analysis
            return inferFromTitle(title);
        }
    }
    
    /**
     * Calculate internship score using weighted keywords
     */
    private double calculateInternshipScore(String combined, String title, String description) {
        double score = 0.0;
        double maxScore = 0.0;
        
        // Title has higher weight
        for (Map.Entry<String, Double> entry : INTERNSHIP_KEYWORDS.entrySet()) {
            double weight = entry.getValue();
            maxScore += weight;
            
            if (title.contains(entry.getKey())) {
                score += weight * 1.5; // Title matches are more important
            } else if (description.contains(entry.getKey())) {
                score += weight;
            }
        }
        
        return maxScore > 0 ? Math.min(score / maxScore, 1.0) : 0.0;
    }
    
    /**
     * Calculate full-time score using weighted keywords
     */
    private double calculateFullTimeScore(String combined, String title, String description) {
        double score = 0.0;
        double maxScore = 0.0;
        
        for (Map.Entry<String, Double> entry : FULL_TIME_KEYWORDS.entrySet()) {
            double weight = entry.getValue();
            maxScore += weight;
            
            if (title.contains(entry.getKey())) {
                score += weight * 1.5;
            } else if (description.contains(entry.getKey())) {
                score += weight;
            }
        }
        
        return maxScore > 0 ? Math.min(score / maxScore, 1.0) : 0.0;
    }
    
    /**
     * Apply negative indicators to reduce confidence
     */
    private double applyNegativeIndicators(String text, double score, List<String> negativeIndicators) {
        for (String indicator : negativeIndicators) {
            if (text.contains(indicator)) {
                score *= 0.5; // Reduce score by 50%
            }
        }
        return score;
    }
    
    /**
     * Infer job type from title patterns
     */
    private JobType inferFromTitle(String title) {
        if (title == null || title.isBlank()) {
            return JobType.FULL_TIME;
        }
        
        String lowerTitle = title.toLowerCase();
        
        // Strong internship indicators in title
        if (lowerTitle.contains("intern") || lowerTitle.contains("trainee")) {
            return JobType.INTERNSHIP;
        }
        
        // Default to full-time for most professional roles
        return JobType.FULL_TIME;
    }
    
    /**
     * Normalize text for better matching
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    /**
     * Get confidence score for classification
     */
    public double getClassificationConfidence(JobListing job) {
        String title = normalizeText(job.getTitle());
        String description = normalizeText(job.getDescription());
        String combined = title + " " + description;
        
        double internshipScore = calculateInternshipScore(combined, title, description);
        double fullTimeScore = calculateFullTimeScore(combined, title, description);
        
        return Math.max(internshipScore, fullTimeScore);
    }
    
    /**
     * Check if job is internship with confidence threshold
     */
    public boolean isInternship(JobListing job, double confidenceThreshold) {
        JobType type = classifyJob(job);
        double confidence = getClassificationConfidence(job);
        return (type == JobType.INTERNSHIP || type == JobType.BOTH) && confidence >= confidenceThreshold;
    }
    
    /**
     * Check if job is full-time with confidence threshold
     */
    public boolean isFullTime(JobListing job, double confidenceThreshold) {
        JobType type = classifyJob(job);
        double confidence = getClassificationConfidence(job);
        return (type == JobType.FULL_TIME || type == JobType.BOTH) && confidence >= confidenceThreshold;
    }
}

