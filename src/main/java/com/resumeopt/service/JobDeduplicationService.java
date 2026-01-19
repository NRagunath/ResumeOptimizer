package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to remove duplicate job listings from a list based on title, company and location.
 */
@Service
public class JobDeduplicationService {
    
    public List<JobListing> removeDuplicates(List<JobListing> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> seen = new HashSet<>();
        List<JobListing> uniqueJobs = new ArrayList<>();
        
        for (JobListing job : jobs) {
            String title = job.getTitle() != null ? job.getTitle().toLowerCase().trim() : "";
            String company = job.getCompany() != null ? job.getCompany().toLowerCase().trim() : "";
            String location = job.getLocation() != null ? job.getLocation().toLowerCase().trim() : "";
            
            // Use title+company as primary key, location as secondary
            // Some jobs might have same title/company but different location (valid distinct jobs)
            String key = title + "|" + company + "|" + location;
            
            if (seen.add(key)) {
                uniqueJobs.add(job);
            }
        }
        
        if (uniqueJobs.size() < jobs.size()) {
            System.out.println("Removed " + (jobs.size() - uniqueJobs.size()) + " duplicate jobs");
        }
        
        return uniqueJobs;
    }
}
