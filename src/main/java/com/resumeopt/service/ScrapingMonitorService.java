package com.resumeopt.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to monitor scraping performance and health.
 */
@Service
public class ScrapingMonitorService {
    
    private final Map<String, ScrapingStats> statsMap = new ConcurrentHashMap<>();
    
    public void recordSuccess(String portalName, int jobsFound) {
        statsMap.computeIfAbsent(portalName, k -> new ScrapingStats()).recordSuccess(jobsFound);
        System.out.println("[MONITOR] " + portalName + ": Success (" + jobsFound + " jobs)");
    }
    
    public void recordDataQuality(String portalName, int totalJobs, int completeJobs) {
        statsMap.computeIfAbsent(portalName, k -> new ScrapingStats()).recordDataQuality(totalJobs, completeJobs);
        double qualityScore = totalJobs > 0 ? (double) completeJobs / totalJobs * 100 : 0;
        System.out.println(String.format("[MONITOR] %s: Data Quality %.2f%% (%d/%d complete)", 
            portalName, qualityScore, completeJobs, totalJobs));
        
        if (qualityScore < 80.0) {
            alertFailure(portalName, "Low data quality: " + String.format("%.2f%%", qualityScore));
        }
    }
    
    public void recordFailure(String portalName, String error) {
        statsMap.computeIfAbsent(portalName, k -> new ScrapingStats()).recordFailure(error);
        alertFailure(portalName, error);
    }
    
    private void alertFailure(String portalName, String error) {
        System.err.println("[ALERT] Scraping issue for " + portalName + ": " + error);
        // Integration point for external alerts (Email, Slack, PagerDuty)
    }
    
    public Map<String, ScrapingStats> getStats() {
        return statsMap;
    }
    
    public static class ScrapingStats {
        private long attempts;
        private long successes;
        private long failures;
        private long totalJobsFound;
        private long totalJobsQualityChecked;
        private long totalCompleteJobs;
        private LocalDateTime lastSuccess;
        private String lastError;
        
        public synchronized void recordSuccess(int jobs) {
            attempts++;
            successes++;
            totalJobsFound += jobs;
            lastSuccess = LocalDateTime.now();
        }
        
        public synchronized void recordDataQuality(int total, int complete) {
            totalJobsQualityChecked += total;
            totalCompleteJobs += complete;
        }
        
        public synchronized void recordFailure(String error) {
            attempts++;
            failures++;
            lastError = error;
        }

        public double getSuccessRate() {
            return attempts > 0 ? (double) successes / attempts * 100 : 0;
        }

        public double getDataQualityScore() {
            return totalJobsQualityChecked > 0 ? (double) totalCompleteJobs / totalJobsQualityChecked * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("Attempts: %d, Success: %d (%.1f%%), Fail: %d, Jobs: %d, Quality: %.1f%%, Last Success: %s", 
                attempts, successes, getSuccessRate(), failures, totalJobsFound, getDataQualityScore(), lastSuccess);
        }
    }
}
