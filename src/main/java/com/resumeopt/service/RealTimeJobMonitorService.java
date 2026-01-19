package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.realtime.RealtimeEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time job monitoring service with change detection and notifications
 */
@Service
public class RealTimeJobMonitorService {
    
    @Autowired(required = false)
    private RealtimeEventPublisher eventPublisher;
    
    @Autowired
    private com.resumeopt.repo.JobListingRepository jobListingRepository;
    
    // Track previously seen jobs
    private final Map<String, JobSnapshot> jobSnapshots = new ConcurrentHashMap<>();
    
    /**
     * Monitor for new jobs and changes
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void monitorJobs() {
        try {
            List<JobListing> currentJobs = jobListingRepository.findRecentJobs(
                LocalDateTime.now().minusDays(1)
            );
            
            Set<String> currentJobIds = new HashSet<>();
            
            for (JobListing job : currentJobs) {
                String jobKey = generateJobKey(job);
                currentJobIds.add(jobKey);
                
                JobSnapshot previous = jobSnapshots.get(jobKey);
                
                if (previous == null) {
                    // New job detected
                    handleNewJob(job);
                } else {
                    // Check for changes
                    if (hasJobChanged(previous, job)) {
                        handleJobUpdate(previous.job, job);
                    }
                }
                
                // Update snapshot
                jobSnapshots.put(jobKey, new JobSnapshot(job, LocalDateTime.now()));
            }
            
            // Detect removed jobs
            detectRemovedJobs(currentJobIds);
            
        } catch (Exception e) {
            System.err.println("Error in job monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Handle new job detection
     */
    private void handleNewJob(JobListing job) {
        System.out.println("New job detected: " + job.getTitle() + " at " + job.getCompany());
        
        if (eventPublisher != null) {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "NEW_JOB");
            event.put("job", job);
            event.put("timestamp", LocalDateTime.now());
            eventPublisher.publish("/topic/jobs/new", event);
        }
    }
    
    /**
     * Handle job update
     */
    private void handleJobUpdate(JobListing oldJob, JobListing newJob) {
        List<String> changes = detectChanges(oldJob, newJob);
        
        if (!changes.isEmpty()) {
            System.out.println("Job updated: " + newJob.getTitle() + " - Changes: " + changes);
            
            if (eventPublisher != null) {
                Map<String, Object> event = new HashMap<>();
                event.put("type", "JOB_UPDATED");
                event.put("job", newJob);
                event.put("changes", changes);
                event.put("timestamp", LocalDateTime.now());
                eventPublisher.publish("/topic/jobs/updated", event);
            }
        }
    }
    
    /**
     * Detect changes between old and new job
     */
    private List<String> detectChanges(JobListing oldJob, JobListing newJob) {
        List<String> changes = new ArrayList<>();
        
        if (!Objects.equals(oldJob.getTitle(), newJob.getTitle())) {
            changes.add("title");
        }
        if (!Objects.equals(oldJob.getDescription(), newJob.getDescription())) {
            changes.add("description");
        }
        if (!Objects.equals(oldJob.getLocation(), newJob.getLocation())) {
            changes.add("location");
        }
        if (!Objects.equals(oldJob.getSalaryRange(), newJob.getSalaryRange())) {
            changes.add("salary");
        }
        if (!Objects.equals(oldJob.getApplicationDeadline(), newJob.getApplicationDeadline())) {
            changes.add("deadline");
        }
        if (!Objects.equals(oldJob.getJobType(), newJob.getJobType())) {
            changes.add("jobType");
        }
        
        return changes;
    }
    
    /**
     * Check if job has changed
     */
    private boolean hasJobChanged(JobSnapshot previous, JobListing current) {
        return !Objects.equals(previous.job.getTitle(), current.getTitle()) ||
               !Objects.equals(previous.job.getDescription(), current.getDescription()) ||
               !Objects.equals(previous.job.getLocation(), current.getLocation()) ||
               !Objects.equals(previous.job.getSalaryRange(), current.getSalaryRange()) ||
               !Objects.equals(previous.job.getApplicationDeadline(), current.getApplicationDeadline());
    }
    
    /**
     * Detect removed jobs
     */
    private void detectRemovedJobs(Set<String> currentJobIds) {
        Set<String> removed = new HashSet<>(jobSnapshots.keySet());
        removed.removeAll(currentJobIds);
        
        for (String jobKey : removed) {
            JobSnapshot snapshot = jobSnapshots.remove(jobKey);
            if (snapshot != null) {
                System.out.println("Job removed: " + snapshot.job.getTitle());
                
                if (eventPublisher != null) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("type", "JOB_REMOVED");
                    event.put("job", snapshot.job);
                    event.put("timestamp", LocalDateTime.now());
                    eventPublisher.publish("/topic/jobs/removed", event);
                }
            }
        }
    }
    
    /**
     * Generate unique key for job
     */
    private String generateJobKey(JobListing job) {
        return (job.getApplyUrl() != null ? job.getApplyUrl() : "") +
               "|" + (job.getTitle() != null ? job.getTitle() : "") +
               "|" + (job.getCompany() != null ? job.getCompany() : "");
    }
    
    /**
     * Get monitoring statistics
     */
    public Map<String, Object> getMonitoringStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("trackedJobs", jobSnapshots.size());
        stats.put("lastUpdate", LocalDateTime.now());
        return stats;
    }
    
    /**
     * Job snapshot for tracking
     */
    private static class JobSnapshot {
        final JobListing job;
        final LocalDateTime timestamp;
        
        JobSnapshot(JobListing job, LocalDateTime timestamp) {
            this.job = job;
            this.timestamp = timestamp;
        }
    }
}

