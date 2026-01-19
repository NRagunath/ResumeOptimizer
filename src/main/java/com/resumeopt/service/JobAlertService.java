package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Job alert service for personalized job notifications
 */
@Service
public class JobAlertService {
    
    // Store user alert preferences
    private final Map<String, AlertPreferences> userPreferences = new ConcurrentHashMap<>();
    
    /**
     * Create or update alert preferences for a user
     */
    public void setAlertPreferences(String userId, AlertPreferences preferences) {
        userPreferences.put(userId, preferences);
    }
    
    /**
     * Get alert preferences for a user
     */
    public AlertPreferences getAlertPreferences(String userId) {
        return userPreferences.getOrDefault(userId, new AlertPreferences());
    }
    
    /**
     * Check if a job matches user's alert criteria
     */
    public boolean matchesAlertCriteria(JobListing job, AlertPreferences preferences) {
        if (job == null || preferences == null) {
            return false;
        }
        
        // Check job type
        if (preferences.jobTypes != null && !preferences.jobTypes.isEmpty()) {
            if (job.getJobType() == null || !preferences.jobTypes.contains(job.getJobType())) {
                return false;
            }
        }
        
        // Check skills
        if (preferences.skills != null && !preferences.skills.isEmpty()) {
            String description = job.getDescription() != null ? job.getDescription().toLowerCase() : "";
            boolean hasSkill = preferences.skills.stream()
                .anyMatch(skill -> description.contains(skill.toLowerCase()));
            if (!hasSkill) {
                return false;
            }
        }
        
        // Check location
        if (preferences.locations != null && !preferences.locations.isEmpty()) {
            String location = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
            boolean matchesLocation = preferences.locations.stream()
                .anyMatch(loc -> location.contains(loc.toLowerCase()));
            if (!matchesLocation) {
                return false;
            }
        }
        
        // Check company
        if (preferences.companies != null && !preferences.companies.isEmpty()) {
            String company = job.getCompany() != null ? job.getCompany().toLowerCase() : "";
            boolean matchesCompany = preferences.companies.stream()
                .anyMatch(comp -> company.contains(comp.toLowerCase()));
            if (!matchesCompany) {
                return false;
            }
        }
        
        // Check salary range
        if (preferences.minSalary != null && job.getSalaryRange() != null) {
            Double jobSalary = extractSalaryFromRange(job.getSalaryRange());
            if (jobSalary != null && jobSalary < preferences.minSalary) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Filter jobs that match alert criteria
     */
    public List<JobListing> filterMatchingJobs(List<JobListing> jobs, AlertPreferences preferences) {
        if (jobs == null || preferences == null) {
            return new ArrayList<>();
        }
        
        return jobs.stream()
                .filter(job -> matchesAlertCriteria(job, preferences))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Extract numeric salary from range string
     */
    private Double extractSalaryFromRange(String salaryRange) {
        if (salaryRange == null) {
            return null;
        }
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(salaryRange);
        
        List<Double> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(Double.parseDouble(matcher.group(1)));
        }
        
        if (values.isEmpty()) {
            return null;
        }
        
        // Return average of range or single value
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
    
    /**
     * Alert preferences class
     */
    public static class AlertPreferences {
        public List<com.resumeopt.model.JobType> jobTypes = new ArrayList<>();
        public List<String> skills = new ArrayList<>();
        public List<String> locations = new ArrayList<>();
        public List<String> companies = new ArrayList<>();
        public Double minSalary;
        public boolean emailNotifications = true;
        public boolean realTimeNotifications = true;
    }
}

