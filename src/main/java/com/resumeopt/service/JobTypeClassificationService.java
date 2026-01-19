package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.JobType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class JobTypeClassificationService {
    
    private static final List<String> INTERNSHIP_KEYWORDS = Arrays.asList(
        "intern", "internship", "trainee", "traineeship", "apprentice", 
        "student", "co-op", "coop", "summer intern", "winter intern",
        "graduate intern", "fresher intern", "internship program"
    );
    
    private static final List<String> FULL_TIME_KEYWORDS = Arrays.asList(
        "full time", "full-time", "permanent", "regular", "fulltime",
        "ft", "f/t", "employee", "staff", "full time position"
    );
    
    /**
     * Classifies a job as INTERNSHIP, FULL_TIME, or BOTH based on title and description
     */
    public JobType classifyJob(JobListing job) {
        if (job == null) {
            return JobType.FULL_TIME; // Default
        }
        
        String title = job.getTitle() != null ? job.getTitle().toLowerCase(Locale.ENGLISH) : "";
        String description = job.getDescription() != null ? job.getDescription().toLowerCase(Locale.ENGLISH) : "";
        String combined = title + " " + description;
        
        boolean isInternship = containsKeywords(combined, INTERNSHIP_KEYWORDS);
        boolean isFullTime = containsKeywords(combined, FULL_TIME_KEYWORDS);
        
        if (isInternship && isFullTime) {
            return JobType.BOTH;
        } else if (isInternship) {
            return JobType.INTERNSHIP;
        } else if (isFullTime) {
            return JobType.FULL_TIME;
        } else {
            // Default to FULL_TIME if no clear indication
            // Most jobs are full-time unless explicitly stated as internship
            return JobType.FULL_TIME;
        }
    }
    
    /**
     * Checks if a job is an internship
     */
    public boolean isInternship(JobListing job) {
        JobType type = classifyJob(job);
        return type == JobType.INTERNSHIP || type == JobType.BOTH;
    }
    
    /**
     * Checks if a job is full-time
     */
    public boolean isFullTime(JobListing job) {
        JobType type = classifyJob(job);
        return type == JobType.FULL_TIME || type == JobType.BOTH;
    }
    
    private boolean containsKeywords(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

