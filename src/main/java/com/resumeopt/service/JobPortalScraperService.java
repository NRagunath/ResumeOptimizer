package com.resumeopt.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.Company;
import com.resumeopt.repo.CompanyRepository;

/**
 * Enhanced job portal scraper service with advanced link verification,
 * date filtering, and deep scraping capabilities.
 * Ensures users see only fresh, verified job listings.
 */
@Service
public class JobPortalScraperService {

    @Autowired
    private List<PortalScraper> portalScrapers;

    @Autowired(required = false)
    private AdvancedJobScraperService advancedScraperService;

    @Autowired
    private JobLinkVerificationService linkVerificationService;

    @Autowired
    private JobDateFilterService dateFilterService;

    @Autowired
    private JobFilterService jobFilterService;

    @Autowired
    private CompanyRepository companyRepository;

    @Value("${job.scraping.deep.enabled:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.dateFilter.enabled:true}")
    private boolean dateFilterEnabled;

    @Value("${job.portals.dateFilter.maxDaysOld:7}")
    private int maxDaysOld;

    private static final int MAX_RETRIES = 4;

    // Executor service for parallel scraping
    // Reduced thread pool size to prevent Selenium resource exhaustion
    private final ExecutorService scraperExecutor = Executors.newFixedThreadPool(3);

    /**
     * Enhanced aggregation with link verification and date filtering
     * 
     * @return Fresh, verified job listings from all portals
     */
    @org.springframework.cache.annotation.Cacheable("portalScrape")
    public List<JobListing> aggregateFromPortals() {
        List<JobListing> collectedJobs = Collections.synchronizedList(new ArrayList<>());
        Set<String> seenHashes = ConcurrentHashMap.newKeySet();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        System.out.println("========================================");
        System.out.println("STARTING JOB SCRAPING FROM ALL PORTALS");
        System.out.println("========================================");
        System.out.println("Total portals configured: " + portalScrapers.size());
        System.out.println("Deep scraping enabled: " + deepScrapingEnabled);
        System.out.println("Date filtering enabled: " + dateFilterEnabled);
        System.out.println("Max days old: " + maxDaysOld);
        System.out.println("========================================\n");

        Map<String, String> scrapStats = new ConcurrentHashMap<>();

        for (PortalScraper scraper : portalScrapers) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String portalName = scraper.getPortalName();
                if (!scraper.isEnabled()) {
                    System.out.println("⏭️  Skipping disabled portal: " + portalName);
                    scrapStats.put(portalName, "❌ Disabled");
                    return;
                }

                try {
                    System.out.println("🔍 Scraping from " + portalName + "...");
                    long startTime = System.currentTimeMillis();
                    List<JobListing> listings = scrapeWithRetry(scraper, MAX_RETRIES);
                    long duration = System.currentTimeMillis() - startTime;

                    if (listings.isEmpty()) {
                        System.out.println("⚠️  No jobs found from " + portalName + " (took " + duration + "ms)");
                        scrapStats.put(portalName, "⚠️  0 jobs found");
                        // Don't return here - continue to allow other scrapers to run
                    } else {
                        System.out.println(
                                "✅ " + portalName + " returned " + listings.size() + " jobs (took " + duration + "ms)");
                    }

                    int rawCount = listings.size();

                    // Enrich with posted dates if missing
                    dateFilterService.enrichWithPostedDates(listings);

                    // Apply date filtering first to reduce processing load
                    if (dateFilterEnabled) {
                        listings = dateFilterService.filterByDateRange(listings);
                    }

                    // Verify links if enabled (can be slow, so maybe skip for initial aggregation)
                    // listings = linkVerificationService.verifyLinks(listings);

                    synchronized (collectedJobs) {
                        collectedJobs.addAll(listings);
                    }

                    scrapStats.put(portalName, "✅ " + listings.size() + " jobs");

                } catch (Exception e) {
                    System.err.println("❌ Error scraping " + portalName + ": " + e.getMessage());
                    scrapStats.put(portalName, "❌ Error: " + e.getMessage());
                    // e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // Wait for all scrapers to finish with a timeout
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(45, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Scraping aggregation timed out or interrupted: " + e.getMessage());
        }

        System.out.println("\n========================================");
        System.out.println("   AGGREGATION SUMMARY");
        System.out.println("========================================");
        scrapStats.forEach((portal, status) -> System.out.println(String.format("%-15s: %s", portal, status)));
        System.out.println("----------------------------------------");
        System.out.println("Total raw jobs collected: " + collectedJobs.size());

        // Deduplicate
        List<JobListing> aggregated = new ArrayList<>();
        for (JobListing job : collectedJobs) {
            String hash = job.getTitle() + "|" + job.getCompany(); // Simple hash
            if (seenHashes.add(hash)) {
                aggregated.add(job);
            }
        }

        System.out.println("After deduplication: " + aggregated.size());

        // Final date filter check
        int beforeFinalFilter = aggregated.size();
        if (dateFilterEnabled) {
            aggregated = dateFilterService.filterByDateRange(aggregated);
            System.out.println("Final date filter: " + beforeFinalFilter + " -> " + aggregated.size() + " jobs");
        }

        return aggregated;
    }

    @org.springframework.cache.annotation.CacheEvict(value = "portalScrape", allEntries = true)
    public void clearCache() {
        System.out.println("Clearing portal scrape cache");
    }

    /**
     * Aggregates jobs from job portals
     */
    public List<JobListing> aggregateAllJobs() {
        List<JobListing> portalJobs = aggregateFromPortals();

        return portalJobs;
    }

    /**
     * Gets jobs posted in the last 24 hours only
     */
    public List<JobListing> getFresh24HourJobs() {
        List<JobListing> portalJobs = aggregateFromPortals();
        return dateFilterService.filterLast24Hours(portalJobs);
    }

    /**
     * Gets jobs posted in the last week (excluding last 24 hours)
     */
    public List<JobListing> getWeeklyJobs() {
        List<JobListing> portalJobs = aggregateFromPortals();
        return dateFilterService.filterLastWeekExcluding24Hours(portalJobs);
    }

    /**
     * Scrapes from a specific portal by name
     * 
     * @param portalName Name of the portal to scrape
     * @return List of job listings from that portal
     */
    public List<JobListing> scrapeFromPortal(String portalName) {
        for (PortalScraper scraper : portalScrapers) {
            if (scraper.getPortalName().equalsIgnoreCase(portalName) && scraper.isEnabled()) {
                try {
                    return scrapeWithRetry(scraper, MAX_RETRIES);
                } catch (Exception e) {
                    System.err.println("Failed to scrape from " + portalName + ": " + e.getMessage());
                    return Collections.emptyList();
                }
            }
        }
        System.out.println("Portal not found or disabled: " + portalName);
        return Collections.emptyList();
    }

    /**
     * Implements retry logic with exponential backoff for rate limiting
     */
    private List<JobListing> scrapeWithRetry(PortalScraper scraper, int maxRetries) {
        int attempt = 0;
        long delay = 1000; // Start with 1 second

        while (attempt < maxRetries) {
            try {
                // Add configured delay before request
                if (attempt > 0 || scraper.getRequestDelay() > 0) {
                    Thread.sleep(attempt == 0 ? scraper.getRequestDelay() : delay);
                }

                return scraper.scrapeJobs();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Scraping interrupted for " + scraper.getPortalName());
                return Collections.emptyList();
            } catch (Exception e) {
                attempt++;
                String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

                // Check if it's a rate limit error (HTTP 429)
                if (errorMessage.contains("429") || errorMessage.contains("rate limit")
                        || errorMessage.contains("too many requests")) {
                    System.out.println(
                            "Rate limited by " + scraper.getPortalName() + ", retry " + attempt + "/" + maxRetries);
                    if (attempt >= maxRetries) {
                        System.err.println("Max retries reached for " + scraper.getPortalName());
                        return Collections.emptyList();
                    }
                    delay *= 2; // Exponential backoff
                } else if (errorMessage.contains("404") || errorMessage.contains("not found")) {
                    // Don't retry for 404 errors
                    System.err.println("Page not found for " + scraper.getPortalName() + ": " + e.getMessage());
                    return Collections.emptyList();
                } else if (errorMessage.contains("timeout") || errorMessage.contains("connect")
                        || errorMessage.contains("connection")) {
                    // Retry for connection/timeout issues
                    System.out.println("Connection issue with " + scraper.getPortalName() + ", retry " + attempt + "/"
                            + maxRetries + ": " + e.getMessage());
                    if (attempt >= maxRetries) {
                        System.err.println("Max retries reached for " + scraper.getPortalName());
                        return Collections.emptyList();
                    }
                    delay *= 2; // Exponential backoff
                } else {
                    // For other errors, retry up to maxRetries
                    System.out.println("Scraping error for " + scraper.getPortalName() + ", retry " + attempt + "/"
                            + maxRetries + ": " + e.getMessage());
                    if (attempt >= maxRetries) {
                        System.err.println("Max retries reached for " + scraper.getPortalName());
                        return Collections.emptyList();
                    }
                    delay *= 2; // Exponential backoff
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Generates a hash for duplicate detection
     */
    private String generateHash(JobListing listing) {
        String combined = (listing.getTitle() + listing.getCompany() + listing.getApplyUrl())
                .toLowerCase()
                .replaceAll("\\s+", "");
        return combined;
    }

    private boolean isRealJobListing(JobListing job) {
        if (job == null)
            return false;
        if (job.getTitle() == null || job.getTitle().isBlank())
            return false;
        if (job.getCompany() == null || job.getCompany().isBlank() || job.getCompany().length() < 2)
            return false;
        if (job.getApplyUrl() == null || job.getApplyUrl().isBlank())
            return false;
        if (job.getDescription() == null || job.getDescription().isBlank() || job.getDescription().length() < 80)
            return false;
        if (job.getPostedDate() == null)
            return false;
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(maxDaysOld);
        if (job.getPostedDate().isBefore(cutoff))
            return false;
        if (jobFilterService != null && !jobFilterService.isRelevant(job))
            return false;
        if (!Boolean.TRUE.equals(job.getLinkVerified()))
            return false;
        return true;
    }
}
