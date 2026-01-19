package com.resumeopt.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple scraper health monitoring service to track success/failure rates
 */
@Service
public class ScraperHealthService {

    private final Map<String, ScraperHealth> healthMap = new ConcurrentHashMap<>();

    /**
     * Record a successful scrape
     */
    public void recordSuccess(String portalName, int jobCount) {
        ScraperHealth health = healthMap.computeIfAbsent(portalName, k -> new ScraperHealth(portalName));
        health.recordSuccess(jobCount);
    }

    /**
     * Record a failed scrape
     */
    public void recordFailure(String portalName, String errorMessage) {
        ScraperHealth health = healthMap.computeIfAbsent(portalName, k -> new ScraperHealth(portalName));
        health.recordFailure(errorMessage);
    }

    /**
     * Get health status for a specific portal
     */
    public ScraperHealth getHealth(String portalName) {
        return healthMap.get(portalName);
    }

    /**
     * Get health status for all portals
     */
    public Map<String, ScraperHealth> getAllHealth() {
        return new ConcurrentHashMap<>(healthMap);
    }

    /**
     * Health status for a single scraper
     */
    public static class ScraperHealth {
        private final String portalName;
        private int successCount = 0;
        private int failureCount = 0;
        private int lastJobCount = 0;
        private LocalDateTime lastSuccessTime;
        private LocalDateTime lastFailureTime;
        private String lastError;

        public ScraperHealth(String portalName) {
            this.portalName = portalName;
        }

        public void recordSuccess(int jobCount) {
            this.successCount++;
            this.lastJobCount = jobCount;
            this.lastSuccessTime = LocalDateTime.now();
        }

        public void recordFailure(String errorMessage) {
            this.failureCount++;
            this.lastError = errorMessage;
            this.lastFailureTime = LocalDateTime.now();
        }

        public String getPortalName() {
            return portalName;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public int getLastJobCount() {
            return lastJobCount;
        }

        public LocalDateTime getLastSuccessTime() {
            return lastSuccessTime;
        }

        public LocalDateTime getLastFailureTime() {
            return lastFailureTime;
        }

        public String getLastError() {
            return lastError;
        }

        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total == 0 ? 0.0 : (double) successCount / total * 100;
        }

        public String getStatus() {
            if (successCount == 0 && failureCount == 0) {
                return "UNKNOWN";
            }
            if (lastSuccessTime == null) {
                return "FAILING";
            }
            if (lastFailureTime != null && lastFailureTime.isAfter(lastSuccessTime)) {
                return "DEGRADED";
            }
            if (getSuccessRate() >= 70) {
                return "HEALTHY";
            } else if (getSuccessRate() >= 40) {
                return "DEGRADED";
            } else {
                return "FAILING";
            }
        }
    }
}
