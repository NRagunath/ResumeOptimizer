package com.resumeopt.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.resumeopt.model.JobListing;

/**
 * Service for verifying job application links to ensure they are valid and accessible
 */
@Service
public class JobLinkVerificationService {
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    /**
     * Verifies all job links in the provided list
     */
    public List<JobListing> verifyJobLinks(List<JobListing> jobs) {
        List<JobListing> verifiedJobs = new ArrayList<>();
        List<CompletableFuture<JobListing>> futures = new ArrayList<>();
        
        for (JobListing job : jobs) {
            CompletableFuture<JobListing> future = CompletableFuture.supplyAsync(() -> {
                return verifyJobLink(job);
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all verifications to complete
        for (CompletableFuture<JobListing> future : futures) {
            try {
                JobListing verifiedJob = future.get(30, TimeUnit.SECONDS);
                if (verifiedJob != null) {
                    verifiedJobs.add(verifiedJob);
                }
            } catch (Exception e) {
                System.err.println("Link verification timeout: " + e.getMessage());
            }
        }
        
        System.out.println("Link verification completed. " + verifiedJobs.size() + " out of " + jobs.size() + " jobs have valid links.");
        return verifiedJobs;
    }
    
    /**
     * Verifies a single job link
     */
    public JobListing verifyJobLink(JobListing job) {
        if (job.getApplyUrl() == null || job.getApplyUrl().isBlank()) {
            System.out.println("Skipping job with empty URL: " + job.getTitle());
            return null;
        }
        
        String originalUrl = job.getApplyUrl();
        
        // Skip strict verification for known anti-bot sites if the URL looks valid
        if (isKnownAntiBotSite(originalUrl)) {
            job.setLinkVerified(true);
            return job;
        }
        
        String verifiedUrl = verifyAndFixUrl(originalUrl);
        
        if (verifiedUrl != null) {
            job.setApplyUrl(verifiedUrl);
            job.setLinkVerified(true);
            return job;
        } else {
            System.out.println("Invalid URL for job: " + job.getTitle() + " - " + originalUrl);
            job.setLinkVerified(false);
            return null; // Remove jobs with invalid links
        }
    }
    
    private boolean isKnownAntiBotSite(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("shine.com") || 
               lowerUrl.contains("wellfound.com") || 
               lowerUrl.contains("naukri.com") || 
               lowerUrl.contains("linkedin.com") ||
               lowerUrl.contains("glassdoor.com") ||
               lowerUrl.contains("glassdoor.co.in");
    }
    
    /**
     * Verifies URL and attempts to fix common issues
     */
    private String verifyAndFixUrl(String url) {
        // Clean and normalize URL
        url = cleanUrl(url);
        
        if (!isValidUrl(url)) {
            return null;
        }
        
        // Test URL with multiple attempts
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                UrlTestResult result = testUrl(url);
                
                if (result.isValid) {
                    return result.finalUrl;
                } else if (result.responseCode == 403 || result.responseCode == 429) {
                    // Rate limited, wait and retry
                    Thread.sleep(2000 * (attempt + 1));
                    continue;
                } else if (result.responseCode >= 400) {
                    // Try alternative URL patterns
                    String alternativeUrl = tryAlternativeUrlPatterns(url);
                    if (alternativeUrl != null) {
                        UrlTestResult altResult = testUrl(alternativeUrl);
                        if (altResult.isValid) {
                            return altResult.finalUrl;
                        }
                    }
                    break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error testing URL: " + url + " - " + e.getMessage());
                if (attempt == 2) {
                    break;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Cleans and normalizes URL
     */
    private String cleanUrl(String url) {
        if (url == null) return null;
        
        url = url.trim();
        
        // Remove common prefixes that might be added by scrapers
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        
        // Fix double slashes in path
        url = url.replaceAll("(?<!:)//+", "/");
        
        // Remove tracking parameters that might cause issues
        url = removeTrackingParameters(url);
        
        return url;
    }
    
    /**
     * Removes common tracking parameters that might cause URL issues
     */
    private String removeTrackingParameters(String url) {
        String[] trackingParams = {
            "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term",
            "fbclid", "gclid", "msclkid", "ref", "referrer"
        };
        
        for (String param : trackingParams) {
            url = url.replaceAll("[?&]" + param + "=[^&]*", "");
        }
        
        // Clean up URL ending
        url = url.replaceAll("[?&]$", "");
        
        return url;
    }
    
    /**
     * Basic URL validation
     */
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    /**
     * Tests URL connectivity and follows redirects
     */
    private UrlTestResult testUrl(String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            
            // Set realistic headers
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", 
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setInstanceFollowRedirects(true);
            
            int responseCode = connection.getResponseCode();
            String finalUrl = connection.getURL().toString();
            
            UrlTestResult result = new UrlTestResult();
            result.responseCode = responseCode;
            result.finalUrl = finalUrl;
            result.isValid = (responseCode >= 200 && responseCode < 400);
            
            return result;
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Tries alternative URL patterns for common job portals
     */
    private String tryAlternativeUrlPatterns(String url) {
        // Naukri: Try different URL formats
        if (url.contains("naukri.com")) {
            if (url.contains("/job-listings/")) {
                return url.replace("/job-listings/", "/jobs-");
            }
        }
        
        // Indeed: Try different URL formats
        if (url.contains("indeed.com")) {
            if (!url.contains("/viewjob?jk=")) {
                // Extract job ID and create proper Indeed URL
                String jobId = extractJobId(url, "jk=([^&]+)");
                if (jobId != null) {
                    return "https://www.indeed.com/viewjob?jk=" + jobId;
                }
            }
        }
        
        // LinkedIn: Try public job view
        if (url.contains("linkedin.com")) {
            if (url.contains("/jobs/view/")) {
                String jobId = extractJobId(url, "/jobs/view/([0-9]+)");
                if (jobId != null) {
                    return "https://www.linkedin.com/jobs/view/" + jobId;
                }
            }
        }
        
        // Glassdoor: Try different formats
        if (url.contains("glassdoor.")) {
            if (url.contains("/job-listing/")) {
                return url.replace("/job-listing/", "/jobs/");
            }
        }
        
        return null;
    }
    
    /**
     * Extracts job ID using regex pattern
     */
    private String extractJobId(String url, String pattern) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(url);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // Ignore regex errors
        }
        return null;
    }
    
    /**
     * Result class for URL testing
     */
    private static class UrlTestResult {
        boolean isValid;
        int responseCode;
        String finalUrl;
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}