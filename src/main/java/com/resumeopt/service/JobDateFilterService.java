package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JobDateFilterService {
    
    // Enhanced date patterns for better parsing
    private static final Pattern[] DATE_PATTERNS = {
        Pattern.compile("(\\d{1,2})\\s+(hours?|hrs?)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(days?|d)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(weeks?|w)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(months?|m)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("yesterday", Pattern.CASE_INSENSITIVE),
        Pattern.compile("today", Pattern.CASE_INSENSITIVE),
        Pattern.compile("just now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})"), // MM/dd/yyyy
        Pattern.compile("(\\d{1,2})-(\\d{1,2})-(\\d{4})"), // MM-dd-yyyy
        Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})"), // yyyy-MM-dd
    };
    
    /**
     * Enhanced filter that includes both past 24 hours AND past week jobs
     * This ensures users see fresh jobs (24 hours) and recent jobs (up to 1 week)
     */
    public List<JobListing> filterByDateRange(List<JobListing> jobs) {
        return filterByDateRange(jobs, "week");
    }
    
    /**
     * Filters jobs by date range: "week" (7 days), "month" (30 days), or custom range
     */
    public List<JobListing> filterByDateRange(List<JobListing> jobs, String dateRange) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate;
        
        if ("month".equalsIgnoreCase(dateRange)) {
            cutoffDate = now.minusDays(30);
        } else if ("week".equalsIgnoreCase(dateRange)) {
            cutoffDate = now.minusDays(7);
        } else {
            // Default to week
            cutoffDate = now.minusDays(7);
        }
        
        return jobs.stream()
                .filter(job -> {
                    LocalDateTime postedDate = job.getPostedDate();
                    if (postedDate == null) {
                        // If no posted date, use created date as fallback
                        postedDate = job.getCreatedAt();
                    }
                    if (postedDate == null) {
                        // If still no date, include the job (assume it's recent)
                        return true;
                    }
                    
                    // Include jobs posted after the cutoff date
                    return postedDate.isAfter(cutoffDate);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Filters jobs by custom date range
     */
    public List<JobListing> filterByCustomDateRange(List<JobListing> jobs, LocalDateTime startDate, LocalDateTime endDate) {
        return jobs.stream()
                .filter(job -> {
                    LocalDateTime postedDate = job.getPostedDate();
                    if (postedDate == null) {
                        postedDate = job.getCreatedAt();
                    }
                    if (postedDate == null) {
                        return false; // Exclude jobs without dates for custom range
                    }
                    
                    return (startDate == null || postedDate.isAfter(startDate) || postedDate.isEqual(startDate)) &&
                           (endDate == null || postedDate.isBefore(endDate) || postedDate.isEqual(endDate));
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Filters jobs to show only those posted in the last 24 hours
     */
    public List<JobListing> filterLast24Hours(List<JobListing> jobs) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        
        return jobs.stream()
                .filter(job -> {
                    LocalDateTime postedDate = job.getPostedDate();
                    if (postedDate == null) {
                        postedDate = job.getCreatedAt();
                    }
                    if (postedDate == null) {
                        return false; // Exclude jobs without dates for 24-hour filter
                    }
                    
                    return postedDate.isAfter(oneDayAgo);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Filters jobs to show only those posted in the last week (excluding last 24 hours)
     */
    public List<JobListing> filterLastWeekExcluding24Hours(List<JobListing> jobs) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        LocalDateTime oneWeekAgo = now.minusDays(7);
        
        return jobs.stream()
                .filter(job -> {
                    LocalDateTime postedDate = job.getPostedDate();
                    if (postedDate == null) {
                        postedDate = job.getCreatedAt();
                    }
                    if (postedDate == null) {
                        return false;
                    }
                    
                    return postedDate.isBefore(oneDayAgo) && postedDate.isAfter(oneWeekAgo);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Sets posted date for jobs that don't have one, using various strategies
     */
    public void enrichWithPostedDates(List<JobListing> jobs) {
        LocalDateTime now = LocalDateTime.now();
        
        for (JobListing job : jobs) {
            if (job.getPostedDate() == null) {
                // For sample/demo jobs, set a random date within the valid range
                if (job.getApplyUrl() != null && job.getApplyUrl().contains("example.com")) {
                    // Sample jobs - set to 2-6 days ago (within valid range)
                    int daysAgo = 2 + (int)(Math.random() * 5); // 2-6 days ago
                    job.setPostedDate(now.minusDays(daysAgo));
                } else {
                    // Real jobs - assume posted 3 days ago if no date available
                    job.setPostedDate(now.minusDays(3));
                }
            }
        }
    }
}