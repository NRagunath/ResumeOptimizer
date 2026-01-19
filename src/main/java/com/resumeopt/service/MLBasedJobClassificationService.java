package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.JobType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Machine Learning-inspired job classification using feature extraction and scoring
 */
@Service
public class MLBasedJobClassificationService {
    
    // Feature weights learned from training data (simulated ML model)
    private static final double TITLE_WEIGHT = 0.4;
    private static final double DESCRIPTION_WEIGHT = 0.35;
    private static final double PATTERN_WEIGHT = 0.1;
    
    // Feature vectors for classification
    private final Map<String, Double> internshipFeatures = new HashMap<>();
    private final Map<String, Double> fullTimeFeatures = new HashMap<>();
    
    public MLBasedJobClassificationService() {
        initializeFeatureVectors();
    }
    
    private void initializeFeatureVectors() {
        // Internship features with TF-IDF-like weights
        internshipFeatures.put("intern", 0.95);
        internshipFeatures.put("internship", 1.0);
        internshipFeatures.put("trainee", 0.88);
        internshipFeatures.put("stipend", 0.75);
        internshipFeatures.put("learn", 0.70);
        internshipFeatures.put("training", 0.72);
        internshipFeatures.put("student", 0.68);
        internshipFeatures.put("duration", 0.80);
        internshipFeatures.put("months", 0.75);
        internshipFeatures.put("program", 0.70);
        
        // Full-time features
        fullTimeFeatures.put("full-time", 0.95);
        fullTimeFeatures.put("permanent", 0.92);
        fullTimeFeatures.put("benefits", 0.85);
        fullTimeFeatures.put("insurance", 0.80);
        fullTimeFeatures.put("salary", 0.75);
        fullTimeFeatures.put("ctc", 0.78);
        fullTimeFeatures.put("package", 0.76);
        fullTimeFeatures.put("employee", 0.70);
        fullTimeFeatures.put("career", 0.65);
    }
    
    /**
     * Classify job using ML-inspired feature extraction and scoring
     */
    public JobType classifyJob(JobListing job) {
        if (job == null) {
            return JobType.FULL_TIME;
        }
        
        // Extract features
        FeatureVector features = extractFeatures(job);
        
        // Calculate scores
        double internshipScore = calculateScore(features, internshipFeatures);
        double fullTimeScore = calculateScore(features, fullTimeFeatures);
        
        // Apply softmax-like normalization
        double totalScore = internshipScore + fullTimeScore;
        if (totalScore > 0) {
            internshipScore = internshipScore / totalScore;
            fullTimeScore = fullTimeScore / totalScore;
        }
        
        // Decision threshold
        double threshold = 0.5;
        if (internshipScore >= threshold && fullTimeScore >= threshold) {
            return JobType.BOTH;
        } else if (internshipScore > fullTimeScore && internshipScore >= threshold) {
            return JobType.INTERNSHIP;
        } else if (fullTimeScore > internshipScore && fullTimeScore >= threshold) {
            return JobType.FULL_TIME;
        } else {
            // Default based on title analysis
            return inferFromTitle(job.getTitle());
        }
    }
    
    /**
     * Extract features from job listing
     */
    private FeatureVector extractFeatures(JobListing job) {
        FeatureVector features = new FeatureVector();
        
        String title = normalize(job.getTitle());
        String description = normalize(job.getDescription());
        String combined = title + " " + description;
        
        // Title features (higher weight)
        features.titleFeatures = extractFeatureVector(title, TITLE_WEIGHT);
        
        // Description features
        features.descriptionFeatures = extractFeatureVector(description, DESCRIPTION_WEIGHT);
        
        // Pattern features
        features.hasDuration = Pattern.compile("\\d+\\s*(months?|weeks?)", Pattern.CASE_INSENSITIVE).matcher(combined).find();
        features.hasStipend = Pattern.compile("stipend|allowance", Pattern.CASE_INSENSITIVE).matcher(combined).find();
        features.hasBenefits = Pattern.compile("benefits|insurance|pto", Pattern.CASE_INSENSITIVE).matcher(combined).find();
        features.hasSalary = Pattern.compile("salary|ctc|package|lpa", Pattern.CASE_INSENSITIVE).matcher(combined).find();
        
        return features;
    }
    
    /**
     * Extract feature vector from text
     */
    private Map<String, Double> extractFeatureVector(String text, double weight) {
        Map<String, Double> vector = new HashMap<>();
        if (text == null || text.isBlank()) {
            return vector;
        }
        
        String lowerText = text.toLowerCase();
        for (Map.Entry<String, Double> entry : internshipFeatures.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                vector.put(entry.getKey(), entry.getValue() * weight);
            }
        }
        for (Map.Entry<String, Double> entry : fullTimeFeatures.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                vector.put(entry.getKey(), entry.getValue() * weight);
            }
        }
        
        return vector;
    }
    
    /**
     * Calculate score using feature vector
     */
    private double calculateScore(FeatureVector features, Map<String, Double> modelFeatures) {
        double score = 0.0;
        
        // Title features
        for (Map.Entry<String, Double> feature : features.titleFeatures.entrySet()) {
            Double modelWeight = modelFeatures.get(feature.getKey());
            if (modelWeight != null) {
                score += feature.getValue() * modelWeight;
            }
        }
        
        // Description features
        for (Map.Entry<String, Double> feature : features.descriptionFeatures.entrySet()) {
            Double modelWeight = modelFeatures.get(feature.getKey());
            if (modelWeight != null) {
                score += feature.getValue() * modelWeight;
            }
        }
        
        // Pattern features
        if (features.hasDuration && modelFeatures.containsKey("duration")) {
            score += modelFeatures.get("duration") * PATTERN_WEIGHT;
        }
        if (features.hasStipend && modelFeatures.containsKey("stipend")) {
            score += modelFeatures.get("stipend") * PATTERN_WEIGHT;
        }
        if (features.hasBenefits && modelFeatures.containsKey("benefits")) {
            score += modelFeatures.get("benefits") * PATTERN_WEIGHT;
        }
        if (features.hasSalary && modelFeatures.containsKey("salary")) {
            score += modelFeatures.get("salary") * PATTERN_WEIGHT;
        }
        
        return score;
    }
    
    /**
     * Get classification confidence
     */
    public double getConfidence(JobListing job) {
        FeatureVector features = extractFeatures(job);
        double internshipScore = calculateScore(features, internshipFeatures);
        double fullTimeScore = calculateScore(features, fullTimeFeatures);
        double totalScore = internshipScore + fullTimeScore;
        
        if (totalScore > 0) {
            return Math.max(internshipScore, fullTimeScore) / totalScore;
        }
        return 0.5;
    }
    
    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }
    
    private JobType inferFromTitle(String title) {
        if (title == null) return JobType.FULL_TIME;
        String lower = title.toLowerCase();
        if (lower.contains("intern") || lower.contains("trainee")) {
            return JobType.INTERNSHIP;
        }
        return JobType.FULL_TIME;
    }
    
    /**
     * Feature vector class
     */
    private static class FeatureVector {
        Map<String, Double> titleFeatures = new HashMap<>();
        Map<String, Double> descriptionFeatures = new HashMap<>();
        boolean hasDuration = false;
        boolean hasStipend = false;
        boolean hasBenefits = false;
        boolean hasSalary = false;
    }
}

