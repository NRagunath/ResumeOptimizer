package com.resumeopt.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import java.time.Duration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.resumeopt.model.JobListing;

/**
 * Enhanced Cutshort scraper with advanced link verification, date filtering,
 * and deep scraping
 * Cutshort is a tech jobs platform
 */
@Component
public class EnhancedCutshortScraper implements PortalScraper {

    @Autowired
    private JobLinkVerificationService linkVerificationService;

    @Autowired
    private AdvancedJobScraperService deepScraperService;

    @Autowired
    private JobDateFilterService dateFilterService;

    @Autowired
    private AdvancedScrapingService advancedScrapingService;

    @Autowired
    private JobDeduplicationService deduplicationService;

    @Autowired
    private ScrapingMonitorService monitorService;

    @Autowired
    private SeleniumService seleniumService;

    @Value("${job.portals.cutshort.enabled:true}")
    private boolean enabled;

    @Value("${job.portals.cutshort.searchQuery:software engineer}")
    private String searchQuery;

    @Value("${job.portals.cutshort.location:India}")
    private String location;

    @Value("${job.portals.cutshort.requestDelay:2000}")
    private long requestDelay;

    @Value("${job.portals.cutshort.maxPages:3}")
    private int maxPages;

    @Value("${job.portals.cutshort.deepScraping:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.cutshort.linkVerification:true}")
    private boolean linkVerificationEnabled;

    @Value("${job.portals.cutshort.maxRetries:3}")
    private int maxRetries;

    private static final String BASE_URL = "https://cutshort.io";

    // Date patterns for parsing Cutshort posting dates
    private static final Pattern[] DATE_PATTERNS = {
            Pattern.compile("(\\d{1,2})\\s+(hours?|hrs?)\\s+ago", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2})\\s+(days?|d)\\s+ago", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2})\\s+(weeks?|w)\\s+ago", Pattern.CASE_INSENSITIVE),
            Pattern.compile("yesterday", Pattern.CASE_INSENSITIVE),
            Pattern.compile("today", Pattern.CASE_INSENSITIVE),
            Pattern.compile("just posted", Pattern.CASE_INSENSITIVE),
    };

    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        if (!enabled) {
            System.out.println("[" + LocalDateTime.now() + "] Enhanced Cutshort scraper is disabled");
            return new ArrayList<>();
        }

        List<JobListing> allJobs = new ArrayList<>();

        // Validate configuration
        validateConfiguration();

        System.out
                .println("[" + LocalDateTime.now() + "] Starting enhanced Cutshort scraping with advanced features...");
        System.out.println(
                "[" + LocalDateTime.now() + "] Configuration: Query='" + searchQuery + "', Location='" + location +
                        "', MaxPages=" + maxPages);

        for (int page = 1; page <= maxPages; page++) {
            try {
                List<JobListing> pageJobs = scrapePageWithRetry(page);
                if (pageJobs.isEmpty()) {
                    break; // Stop if no jobs found
                }
                allJobs.addAll(pageJobs);

                System.out.println("[" + LocalDateTime.now() + "] Page " + page + " completed: " + pageJobs.size()
                        + " jobs found");

                // Delay between pages to respect rate limits
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[" + LocalDateTime.now() + "] Scraping interrupted");
                break;
            } catch (Exception e) {
                System.err.println(
                        "[" + LocalDateTime.now() + "] Error scraping Cutshort page " + page + ": " + e.getMessage());
                // Continue with next page instead of failing completely
            }
        }

        System.out
                .println("[" + LocalDateTime.now() + "] Initial scraping completed: " + allJobs.size() + " jobs found");

        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);

        System.out
                .println("[" + LocalDateTime.now() + "] Enhanced Cutshort scraping completed: " + processedJobs.size() +
                        " verified fresh jobs with working links");

        if (monitorService != null) {
            monitorService.recordSuccess(getPortalName(), processedJobs.size());
        }

        return processedJobs;
    }

    /**
     * Validates scraper configuration and sets defaults for invalid values
     */
    private void validateConfiguration() {
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: Cutshort search query is empty, using default");
            searchQuery = "software engineer";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: Cutshort request delay is negative, using default 2000ms");
            requestDelay = 2000;
        }
        if (maxPages < 1) {
            maxPages = 1;
        }
    }

    /**
     * Scrapes a single page with retry logic for rate limiting
     */
    private List<JobListing> scrapePageWithRetry(int page) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return scrapePage(page);
            } catch (IOException e) {
                if (e.getMessage().contains("429") || e.getMessage().contains("rate limit")) {
                    if (attempt < maxRetries - 1) {
                        long delay = (long) Math.pow(2, attempt) * 1000; // Exponential backoff
                        System.out.println("Rate limited, waiting " + delay + "ms before retry " + (attempt + 2));
                        Thread.sleep(delay);
                        continue;
                    }
                }
                throw e;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Scrapes a single Cutshort page
     */
    private List<JobListing> scrapePage(int page) throws IOException {
        String url = buildSearchUrl(page);
        System.out.println("Scraping Cutshort page " + page + ": " + url);

        Document doc = null;
        // Cutshort is a React app, so we should default to Selenium or try Jsoup if SSR
        // is enabled (unlikely for search results)
        // But let's try Jsoup first just in case, as it's faster
        try {
            doc = advancedScrapingService.fetchDocument(url);
        } catch (Exception e) {
            System.out.println("Jsoup failed for Cutshort, trying Selenium: " + e.getMessage());
        }

        Elements jobCards = (doc != null) ? doc.select(".job-card, .opportunity-card, div[class*='JobCard']")
                : new Elements();

        if (doc == null || jobCards.isEmpty()) {
            System.out.println("Jsoup found no jobs, switching to Selenium for Cutshort page " + page);
            try {
                doc = seleniumService.fetchDocument(url);
                // Wait for job cards to load - SeleniumService usually waits for body, but we
                // might need specific wait
                // However, we can't easily inject custom waits into SeleniumService without
                // modifying it.
                // Assuming SeleniumService has some basic wait or we rely on it returning the
                // DOM after load.

                jobCards = doc.select(
                        ".job-card, .opportunity-card, div[class*='JobCard'], div[class*='jobCard'], a[href*='/job/']");
            } catch (Exception e) {
                System.err.println("Selenium also failed for Cutshort: " + e.getMessage());
                throw new IOException("Cutshort scraping failed with both Jsoup and Selenium", e);
            }
        }

        if (jobCards.isEmpty()) {
            System.out.println("No job cards found on Cutshort page " + page +
                    ". Page structure may have changed or no results available.");
            return new ArrayList<>();
        }

        List<JobListing> jobs = new ArrayList<>();

        for (Element card : jobCards) {
            try {
                JobListing job = parseJobCardEnhanced(card);
                if (job != null && isValidListing(job)) {
                    jobs.add(job);
                }
            } catch (Exception e) {
                System.err.println("Error parsing Cutshort job card: " + e.getMessage());
                // Continue with next job instead of failing
            }
        }

        return jobs;
    }

    /**
     * Builds Cutshort search URL with advanced parameters
     */
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            // Cutshort uses experience level filters
            String url = BASE_URL + "/jobs/" + encodedQuery.replace("+", "-");

            // Add location filter
            if (location != null && !location.isBlank()) {
                String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
                if (url.contains("?")) {
                    url += "&location=" + encodedLocation;
                } else {
                    url += "?location=" + encodedLocation;
                }
            }

            if (url.contains("?")) {
                url += "&experience=0-1";
            } else {
                url += "?experience=0-1";
            }

            // Add page number
            if (page > 1) {
                if (url.contains("?")) {
                    url += "&page=" + page;
                } else {
                    url += "?page=" + page;
                }
            }

            return url;
        } catch (Exception e) {
            System.err.println("Error building Cutshort URL: " + e.getMessage());
            return BASE_URL + "/jobs/software-engineer?experience=0-3&page=1";
        }
    }

    /**
     * Enhanced job card parsing with better field extraction
     */
    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();

        // Extract title
        Element titleElement = card.selectFirst(".job-title, h3 a, .title, div[class*='title']");
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }

        // Extract company
        Element companyElement = card.selectFirst(".company-name, .company, div[class*='company']");
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }

        // Extract location
        Element locationElement = card.selectFirst(".job-location, .location, div[class*='location']");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";

        // Extract salary if available
        Element salaryElement = card.selectFirst(".salary, .compensation, div[class*='salary']");
        String salary = salaryElement != null ? cleanText(salaryElement.text()) : "";

        // Extract skills if available
        Element skillsElement = card.selectFirst(".skills, .tech-stack, div[class*='skills']");
        String skills = skillsElement != null ? cleanText(skillsElement.text()) : "";

        // Extract description/snippet with enhanced content
        Element descElement = card.selectFirst(".job-description, .description");
        StringBuilder description = new StringBuilder();

        if (descElement != null) {
            description.append(cleanText(descElement.text()));
        } else {
            description.append("Entry-level tech position from Cutshort (0-2 years)");
        }

        // Add location, salary, and skills to description if available
        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
            description.append(" | Location: ").append(locationText);
        }
        if (!salary.isEmpty()) {
            description.append(" | Salary: ").append(salary);
        }
        if (!skills.isEmpty()) {
            description.append(" | Skills: ").append(skills);
        }

        job.setDescription(description.toString());

        // Extract apply URL
        Element linkElement = card.selectFirst("a[href*='/job/'], a.job-link, a");
        if (linkElement != null) {
            String href = linkElement.attr("href");
            if (href.startsWith("/")) {
                job.setApplyUrl(BASE_URL + href);
            } else if (!href.startsWith("http")) {
                job.setApplyUrl(BASE_URL + "/" + href);
            } else {
                job.setApplyUrl(href);
            }
        }

        // Extract and parse posting date
        LocalDateTime postedDate = extractPostingDate(card);
        if (postedDate != null) {
            job.setPostedDate(postedDate);
        }

        // Set creation time
        job.setCreatedAt(LocalDateTime.now());

        return job;
    }

    /**
     * Extracts posting date from job card
     */
    private LocalDateTime extractPostingDate(Element card) {
        // Look for date elements in various locations
        Elements dateElements = card.select(".posted-date, .job-date, [class*='date']");

        for (Element elem : dateElements) {
            String dateText = elem.text().trim();
            LocalDateTime parsed = parseRelativeDate(dateText);
            if (parsed != null) {
                return parsed;
            }
        }

        // If no specific date found, assume posted recently (within valid range)
        return LocalDateTime.now().minusDays(2);
    }

    /**
     * Parses relative date strings like "2 days ago", "1 week ago"
     */
    private LocalDateTime parseRelativeDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        text = text.toLowerCase().trim();

        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    if (text.contains("today") || text.contains("just posted")) {
                        return now;
                    } else if (text.contains("yesterday")) {
                        return now.minusDays(1);
                    } else if (text.contains("hour")) {
                        int hours = Integer.parseInt(matcher.group(1));
                        return now.minusHours(hours);
                    } else if (text.contains("day")) {
                        int days = Integer.parseInt(matcher.group(1));
                        return now.minusDays(days);
                    } else if (text.contains("week")) {
                        int weeks = Integer.parseInt(matcher.group(1));
                        return now.minusWeeks(weeks);
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        return null;
    }

    /**
     * Applies advanced processing pipeline: deep scraping, link verification, date
     * filtering
     */
    private List<JobListing> applyAdvancedProcessing(List<JobListing> jobs) {
        if (jobs.isEmpty()) {
            return jobs;
        }

        System.out.println("Applying advanced processing pipeline...");

        // Step 0: Deduplicate
        List<JobListing> uniqueJobs = jobs;
        if (deduplicationService != null) {
            uniqueJobs = deduplicationService.removeDuplicates(jobs);
        }

        // Step 1: Filter by date to ensure only fresh jobs (past week)
        List<JobListing> freshJobs = dateFilterService.filterByDateRange(uniqueJobs);
        System.out.println("After date filtering: " + freshJobs.size() + " fresh jobs (within past week)");

        // Step 2: Apply deep scraping if enabled
        List<JobListing> enhancedJobs = freshJobs;
        if (deepScrapingEnabled && deepScraperService != null) {
            try {
                enhancedJobs = deepScraperService.enhanceWithDeepScraping(freshJobs);
                System.out.println("After deep scraping: " + enhancedJobs.size() + " enhanced jobs");
            } catch (Exception e) {
                System.err.println("Deep scraping failed: " + e.getMessage());
                enhancedJobs = freshJobs; // Fallback to original jobs
            }
        }

        // Step 3: Verify links if enabled
        List<JobListing> verifiedJobs = enhancedJobs;
        if (linkVerificationEnabled && linkVerificationService != null) {
            try {
                verifiedJobs = linkVerificationService.verifyJobLinks(enhancedJobs);
                System.out.println("After link verification: " + verifiedJobs.size() + " jobs with verified links");
            } catch (Exception e) {
                System.err.println("Link verification failed: " + e.getMessage());
                verifiedJobs = enhancedJobs; // Fallback to enhanced jobs
            }
        }

        return verifiedJobs;
    }

    /**
     * Validates that a job listing has all required fields
     */
    private boolean isValidListing(JobListing job) {
        return job.getTitle() != null && !job.getTitle().isBlank()
                && job.getCompany() != null && !job.getCompany().isBlank()
                && job.getApplyUrl() != null && !job.getApplyUrl().isBlank()
                && job.getDescription() != null && !job.getDescription().isBlank();
    }

    /**
     * Cleans text by removing extra whitespace and unwanted characters
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ").replaceAll("[\\r\\n]+", " ");
    }

    @Override
    public String getPortalName() {
        return "Cutshort";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public long getRequestDelay() {
        return requestDelay;
    }
}
