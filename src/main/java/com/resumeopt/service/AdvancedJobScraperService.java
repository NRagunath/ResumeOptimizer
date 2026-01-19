package com.resumeopt.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resumeopt.model.JobListing;

/**
 * Advanced job scraper service with enhanced link verification, 
 * date filtering, and deep scraping capabilities
 */
@Service
public class AdvancedJobScraperService {
    
    @Autowired
    private JobDateFilterService dateFilterService;
    
    @Value("${job.scraping.deep.enabled:true}")
    private boolean deepScrapingEnabled;
    
    @Value("${job.scraping.deep.maxJobsPerPortal:50}")
    private int maxJobsPerPortal;
    
    @Value("${job.scraping.deep.requestDelay:2000}")
    private long deepRequestDelay;
    
    @Value("${job.scraping.deep.timeout:20000}")
    private int deepTimeout;
    
    @Value("${job.scraping.deep.maxRetries:3}")
    private int maxRetries;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    // Date patterns for parsing job posting dates
    private static final Pattern[] DATE_PATTERNS = {
        Pattern.compile("(\\d{1,2})\\s+(hours?|hrs?)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(days?|d)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(weeks?|w)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(months?|m)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("yesterday", Pattern.CASE_INSENSITIVE),
        Pattern.compile("today", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})"), // MM/dd/yyyy
        Pattern.compile("(\\d{1,2})-(\\d{1,2})-(\\d{4})"), // MM-dd-yyyy
        Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})"), // yyyy-MM-dd
    };
    
    /**
     * Enhances job listings with deep scraping to get more accurate data
     */
    public List<JobListing> enhanceWithDeepScraping(List<JobListing> jobs) {
        if (!deepScrapingEnabled || jobs.isEmpty()) {
            return jobs;
        }
        
        List<JobListing> enhancedJobs = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Limit the number of jobs to process to avoid overwhelming servers
        int jobsToProcess = Math.min(jobs.size(), maxJobsPerPortal);
        
        for (int i = 0; i < jobsToProcess; i++) {
            JobListing job = jobs.get(i);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    JobListing enhancedJob = deepScrapeJob(job);
                    if (enhancedJob != null && isJobWithinDateRange(enhancedJob)) {
                        enhancedJobs.add(enhancedJob);
                    }
                } catch (Exception e) {
                    System.err.println("Deep scraping failed for job: " + job.getTitle() + " - " + e.getMessage());
                    // Add original job if deep scraping fails
                    if (isJobWithinDateRange(job)) {
                        enhancedJobs.add(job);
                    }
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all futures to complete with timeout
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Deep scraping timeout or error: " + e.getMessage());
        }
        
        System.out.println("Deep scraping completed. Enhanced " + enhancedJobs.size() + " jobs out of " + jobsToProcess);
        return new ArrayList<>(enhancedJobs);
    }
    
    /**
     * Performs deep scraping on individual job listing
     */
    private JobListing deepScrapeJob(JobListing job) throws IOException, InterruptedException {
        if (job.getApplyUrl() == null || job.getApplyUrl().isBlank()) {
            return job;
        }
        
        // Verify and enhance the job URL
        String verifiedUrl = verifyAndEnhanceJobUrl(job.getApplyUrl());
        if (verifiedUrl == null) {
            return null; // Skip invalid URLs
        }
        
        job.setApplyUrl(verifiedUrl);
        job.setLinkVerified(true);
        
        // Add delay to respect rate limits
        Thread.sleep(deepRequestDelay);
        
        try {
            Document doc = Jsoup.connect(verifiedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .timeout(deepTimeout)
                    .followRedirects(true)
                    .get();
            
            // Extract enhanced job details
            enhanceJobDetails(job, doc);
            
            // Extract and parse posting date
            LocalDateTime postedDate = extractPostedDate(doc);
            if (postedDate != null) {
                job.setPostedDate(postedDate);
            }
            
            return job;
            
        } catch (IOException e) {
            System.err.println("Failed to deep scrape job: " + verifiedUrl + " - " + e.getMessage());
            return job; // Return original job if deep scraping fails
        }
    }
    
    /**
     * Verifies job URL and enhances it for better reliability
     */
    private String verifyAndEnhanceJobUrl(String url) {
        try {
            // Clean and normalize URL
            url = url.trim();
            if (!url.startsWith("http")) {
                return null;
            }
            
            // Test URL connectivity with retries
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("HEAD");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("User-Agent", 
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                    
                    int responseCode = connection.getResponseCode();
                    
                    if (responseCode == 200) {
                        return url; // URL is valid
                    } else if (responseCode == 301 || responseCode == 302) {
                        // Follow redirect
                        String redirectUrl = connection.getHeaderField("Location");
                        if (redirectUrl != null) {
                            return redirectUrl;
                        }
                    } else if (responseCode == 403 || responseCode == 429) {
                        // Rate limited, wait and retry
                        Thread.sleep(2000 * (attempt + 1));
                        continue;
                    }
                    
                    connection.disconnect();
                } catch (Exception e) {
                    if (attempt == maxRetries - 1) {
                        System.err.println("URL verification failed after retries: " + url);
                        return null;
                    }
                    Thread.sleep(1000 * (attempt + 1));
                }
            }
            
            return null; // URL verification failed
            
        } catch (Exception e) {
            System.err.println("Error verifying URL: " + url + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Enhances job details by extracting additional information from job page
     */
    private void enhanceJobDetails(JobListing job, Document doc) {
        try {
            // Extract more detailed description
            Elements descElements = doc.select(
                ".job-description, .jobsearch-jobDescriptionText, " +
                ".description, .job-details, .job-summary, " +
                "[class*='description'], [class*='detail'], [class*='summary']"
            );
            
            if (!descElements.isEmpty()) {
                StringBuilder enhancedDesc = new StringBuilder();
                for (Element elem : descElements) {
                    String text = elem.text().trim();
                    if (text.length() > 50) { // Only add substantial content
                        enhancedDesc.append(text).append(" ");
                        if (enhancedDesc.length() > 1000) break; // Limit description length
                    }
                }
                
                if (enhancedDesc.length() > 0) {
                    job.setDescription(enhancedDesc.toString().trim());
                }
            }
            
            // Extract salary information and add to description
            Elements salaryElements = doc.select(
                ".salary, .compensation, .pay, " +
                "[class*='salary'], [class*='compensation'], [class*='pay']"
            );
            
            String salaryInfo = null;
            for (Element elem : salaryElements) {
                String salaryText = elem.text().trim();
                if (salaryText.matches(".*\\d+.*") && salaryText.length() < 100) {
                    salaryInfo = salaryText;
                    break;
                }
            }
            
            // Extract location information and add to description
            Elements locationElements = doc.select(
                ".location, .job-location, .workplace-location, " +
                "[class*='location'], [data-testid*='location']"
            );
            
            String locationInfo = null;
            for (Element elem : locationElements) {
                String locationText = elem.text().trim();
                if (locationText.length() > 2 && locationText.length() < 100) {
                    locationInfo = locationText;
                    break;
                }
            }
            
            // Append salary and location to description if found
            if (salaryInfo != null || locationInfo != null) {
                String currentDesc = job.getDescription();
                StringBuilder enhancedDesc = new StringBuilder(currentDesc);
                if (locationInfo != null) {
                    enhancedDesc.append(" | Location: ").append(locationInfo);
                }
                if (salaryInfo != null) {
                    enhancedDesc.append(" | Salary: ").append(salaryInfo);
                }
                job.setDescription(enhancedDesc.toString());
            }
            
            // Extract company information if not already set
            if (job.getCompany() == null || job.getCompany().isBlank()) {
                Elements companyElements = doc.select(
                    ".company, .employer, .company-name, " +
                    "[class*='company'], [class*='employer']"
                );
                
                for (Element elem : companyElements) {
                    String companyText = elem.text().trim();
                    if (companyText.length() > 1 && companyText.length() < 100) {
                        job.setCompany(companyText);
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error enhancing job details: " + e.getMessage());
        }
    }
    
    /**
     * Extracts posting date from job page
     */
    private LocalDateTime extractPostedDate(Document doc) {
        try {
            // Look for date elements
            Elements dateElements = doc.select(
                ".date, .posted-date, .job-date, .publish-date, " +
                "[class*='date'], [class*='posted'], [class*='time'], " +
                "time, .timestamp"
            );
            
            for (Element elem : dateElements) {
                // Try datetime attribute first
                String datetime = elem.attr("datetime");
                if (!datetime.isEmpty()) {
                    LocalDateTime parsed = parseDateTime(datetime);
                    if (parsed != null) return parsed;
                }
                
                // Try element text
                String dateText = elem.text().trim();
                LocalDateTime parsed = parseRelativeDate(dateText);
                if (parsed != null) return parsed;
            }
            
            // Look in page text for date patterns
            String pageText = doc.text();
            return parseRelativeDate(pageText);
            
        } catch (Exception e) {
            System.err.println("Error extracting posted date: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses ISO datetime strings
     */
    private LocalDateTime parseDateTime(String datetime) {
        try {
            return LocalDateTime.parse(datetime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(datetime + "T00:00:00", DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
    
    /**
     * Parses relative date strings like "2 days ago", "1 week ago"
     */
    private LocalDateTime parseRelativeDate(String text) {
        LocalDateTime now = LocalDateTime.now();
        
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    if (text.toLowerCase().contains("today")) {
                        return now;
                    } else if (text.toLowerCase().contains("yesterday")) {
                        return now.minusDays(1);
                    } else if (text.contains("hour")) {
                        int hours = Integer.parseInt(matcher.group(1));
                        return now.minusHours(hours);
                    } else if (text.contains("day") || text.contains(" d ")) {
                        int days = Integer.parseInt(matcher.group(1));
                        return now.minusDays(days);
                    } else if (text.contains("week")) {
                        int weeks = Integer.parseInt(matcher.group(1));
                        return now.minusWeeks(weeks);
                    } else if (text.contains("month")) {
                        int months = Integer.parseInt(matcher.group(1));
                        return now.minusMonths(months);
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Checks if job is within the required date range (past 24 hours to 1 week)
     */
    private boolean isJobWithinDateRange(JobListing job) {
        LocalDateTime postedDate = job.getPostedDate();
        if (postedDate == null) {
            // If no date available, assume it's valid for now
            return true;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        LocalDateTime oneWeekAgo = now.minusDays(7);
        
        // Job should be posted between 1 day ago and 1 week ago
        return postedDate.isBefore(oneDayAgo) && postedDate.isAfter(oneWeekAgo);
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