package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Advanced duplicate detection using fuzzy matching, similarity scoring, and multiple strategies
 */
@Service
public class SmartDuplicateDetectionService {
    
    private static final double TITLE_SIMILARITY_THRESHOLD = 0.85;
    private static final double COMPANY_SIMILARITY_THRESHOLD = 0.90;
    private static final double COMBINED_SIMILARITY_THRESHOLD = 0.80;
    
    /**
     * Remove duplicates from job list using advanced fuzzy matching
     */
    public List<JobListing> removeDuplicates(List<JobListing> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return jobs;
        }
        
        List<JobListing> unique = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();
        
        for (JobListing job : jobs) {
            if (job == null) {
                continue;
            }
            
            // Strategy 1: Exact URL match (highest confidence)
            if (job.getApplyUrl() != null && !job.getApplyUrl().isBlank()) {
                String urlHash = normalizeUrl(job.getApplyUrl());
                if (seenHashes.contains("url:" + urlHash)) {
                    continue; // Skip duplicate
                }
                seenHashes.add("url:" + urlHash);
            }
            
            // Strategy 2: Fuzzy matching with existing jobs
            boolean isDuplicate = false;
            for (JobListing existing : unique) {
                if (isDuplicateJob(job, existing)) {
                    isDuplicate = true;
                    break;
                }
            }
            
            if (!isDuplicate) {
                unique.add(job);
                // Add to hash set for fast lookup
                addToHashSet(job, seenHashes);
            }
        }
        
        return unique;
    }
    
    /**
     * Check if two jobs are duplicates using multiple strategies
     */
    public boolean isDuplicateJob(JobListing job1, JobListing job2) {
        if (job1 == null || job2 == null) {
            return false;
        }
        
        // Strategy 1: Exact URL match
        if (job1.getApplyUrl() != null && job2.getApplyUrl() != null &&
            !job1.getApplyUrl().isBlank() && !job2.getApplyUrl().isBlank()) {
            if (normalizeUrl(job1.getApplyUrl()).equals(normalizeUrl(job2.getApplyUrl()))) {
                return true;
            }
        }
        
        // Strategy 2: Title and Company similarity
        double titleSimilarity = calculateSimilarity(
            normalizeText(job1.getTitle()),
            normalizeText(job2.getTitle())
        );
        
        double companySimilarity = calculateSimilarity(
            normalizeText(job1.getCompany()),
            normalizeText(job2.getCompany())
        );
        
        if (titleSimilarity >= TITLE_SIMILARITY_THRESHOLD && 
            companySimilarity >= COMPANY_SIMILARITY_THRESHOLD) {
            return true;
        }
        
        // Strategy 3: Combined similarity score
        double combinedSimilarity = (titleSimilarity * 0.6) + (companySimilarity * 0.4);
        if (combinedSimilarity >= COMBINED_SIMILARITY_THRESHOLD) {
            // Additional check: description similarity
            double descSimilarity = calculateSimilarity(
                normalizeText(job1.getDescription()),
                normalizeText(job2.getDescription())
            );
            if (descSimilarity > 0.7) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Calculate similarity between two strings using Levenshtein distance
     */
    private double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        if (str1.isBlank() || str2.isBlank()) {
            return 0.0;
        }
        
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(str1, str2);
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + 1
                    );
                }
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Normalize text for comparison
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    /**
     * Normalize URL for comparison
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String normalized = urlObj.getProtocol() + "://" + urlObj.getHost() + urlObj.getPath();
            // Remove query parameters and fragments for comparison
            return normalized.toLowerCase().replaceAll("/$", "");
        } catch (Exception e) {
            return url.toLowerCase().split("\\?")[0].split("#")[0];
        }
    }
    
    /**
     * Add job to hash set for fast lookup
     */
    private void addToHashSet(JobListing job, Set<String> seenHashes) {
        if (job.getTitle() != null && job.getCompany() != null) {
            String titleCompanyHash = normalizeText(job.getTitle()) + "|" + normalizeText(job.getCompany());
            seenHashes.add("titlecompany:" + titleCompanyHash);
        }
    }
    
    /**
     * Group similar jobs together
     */
    public Map<String, List<JobListing>> groupSimilarJobs(List<JobListing> jobs) {
        Map<String, List<JobListing>> groups = new HashMap<>();
        
        for (JobListing job : jobs) {
            boolean added = false;
            
            for (Map.Entry<String, List<JobListing>> entry : groups.entrySet()) {
                JobListing representative = entry.getValue().get(0);
                if (isDuplicateJob(job, representative)) {
                    entry.getValue().add(job);
                    added = true;
                    break;
                }
            }
            
            if (!added) {
                String key = generateKey(job);
                groups.put(key, new ArrayList<>(List.of(job)));
            }
        }
        
        return groups;
    }
    
    /**
     * Generate a key for grouping
     */
    private String generateKey(JobListing job) {
        return normalizeText(job.getTitle()) + "|" + normalizeText(job.getCompany());
    }
}

