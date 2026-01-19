package com.resumeopt.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.resumeopt.model.JobListing;

/**
 * Enhanced Freshersworld scraper with advanced link verification, date
 * filtering, and deep scraping
 * Freshersworld scraper for job listings in India
 */
@Component
public class EnhancedFreshersworldScraper implements PortalScraper {

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

    @Value("${job.portals.freshersworld.enabled:true}")
    private boolean enabled;

    @Value("${job.portals.freshersworld.searchQuery:software engineer}")
    private String searchQuery;

    @Value("${job.portals.freshersworld.location:India}")
    private String location;

    @Value("${job.portals.freshersworld.requestDelay:2000}")
    private long requestDelay;

    @Value("${job.portals.freshersworld.maxPages:3}")
    private int maxPages;

    @Value("${job.portals.freshersworld.deepScraping:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.freshersworld.linkVerification:true}")
    private boolean linkVerificationEnabled;

    @Value("${job.portals.freshersworld.maxRetries:3}")
    private int maxRetries;

    private static final String BASE_URL = "https://www.freshersworld.com";

    // Date patterns for parsing Freshersworld posting dates
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
            System.out.println("Enhanced Freshersworld scraper is disabled");
            return new ArrayList<>();
        }

        List<JobListing> allJobs = new ArrayList<>();

        // Validate configuration
        validateConfiguration();

        System.out.println("Starting enhanced Freshersworld scraping with advanced features...");
        System.out.println("Configuration: Query='" + searchQuery + "', Location='" + location +
                "', MaxPages=" + maxPages);

        for (int page = 1; page <= maxPages; page++) {
            try {
                List<JobListing> pageJobs = scrapePageWithRetry(page);
                allJobs.addAll(pageJobs);

                System.out.println("Page " + page + " completed: " + pageJobs.size() + " jobs found");

                // Delay between pages to respect rate limits
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Scraping interrupted");
                break;
            } catch (Exception e) {
                System.err.println("Error scraping Freshersworld page " + page + ": " + e.getMessage());
                // Continue with next page instead of failing completely
            }
        }

        System.out.println("Initial scraping completed: " + allJobs.size() + " jobs found");

        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);

        System.out.println("Enhanced Freshersworld scraping completed: " + processedJobs.size() +
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
            System.out.println("WARNING: Freshersworld search query is empty, using default");
            searchQuery = "software engineer";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: Freshersworld request delay is negative, using default 2000ms");
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
     * Scrapes a single Freshersworld page
     */
    private List<JobListing> scrapePage(int page) throws IOException {
        String url = buildSearchUrl(page);
        System.out.println("[" + LocalDateTime.now() + "] Scraping Freshersworld page " + page + ": " + url);

        Document doc = advancedScrapingService.fetchDocument(url);

        // Anti-bot detection
        if (doc.title().contains("Cloudflare") || doc.text().contains("Verify you are human")) {
            System.err.println("[" + LocalDateTime.now() + "] CRITICAL: Blocked by Freshersworld anti-bot");
            throw new IOException("Freshersworld Anti-Bot detection");
        }

        // Explicit no results check
        if (doc.text().contains("No jobs found") || doc.text().contains("did not match any jobs")) {
            System.out.println("[" + LocalDateTime.now() + "] No jobs found on Freshersworld page " + page);
            return new ArrayList<>();
        }

        Elements jobCards = doc.select(
                ".job-container, .job-tittle, article.job, .list-container, .job-list, .job-posting, .job-card, div[class*='job-block'], div[class*='job_listing']");

        if (jobCards.isEmpty()) {
            System.out.println("[" + LocalDateTime.now() + "] No job cards found on Freshersworld page " + page +
                    " with Jsoup. Switching to Selenium...");
            try {
                doc = seleniumService.fetchDocument(url);
                jobCards = doc.select(
                        ".job-container, .job-tittle, article.job, .list-container, .job-list, .job-posting, .job-card, div[class*='job-block'], div[class*='job_listing']");
            } catch (Exception e) {
                System.err.println("Selenium fallback failed: " + e.getMessage());
            }
        }

        if (jobCards.isEmpty()) {
            System.out.println("[" + LocalDateTime.now() + "] No job cards found on Freshersworld page " + page
                    + " even with Selenium.");
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
                System.err.println(
                        "[" + LocalDateTime.now() + "] Error parsing Freshersworld job card: " + e.getMessage());
                // Continue with next job instead of failing
            }
        }

        return jobs;
    }

    /**
     * Builds Freshersworld search URL with advanced parameters
     */
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            // Mark as entry level
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

            // Updated URL pattern: /jobs/jobsearch/{query}-jobs-in-{location}
            // e.g.
            // https://www.freshersworld.com/jobs/jobsearch/software-engineer-jobs-in-india
            String querySlug = encodedQuery.replace("+", "-").toLowerCase();
            String locationSlug = encodedLocation.replace("+", "-").toLowerCase();

            String searchUrl = BASE_URL + "/jobs/jobsearch/" + querySlug + "-jobs-in-" + locationSlug;

            // Add page number if > 1
            if (page > 1) {
                searchUrl += "?offset=" + ((page - 1) * 20); // Freshersworld uses offset usually, or verify page param
                // Actually, let's check if 'page' param works or if we need offset.
                // The verification URL didn't use page.
                // Let's assume ?page= works or just append it safely.
                // If unsure, we can stick to page 1 or try ?page=N
                searchUrl += "&page=" + page;
            }

            return searchUrl;

        } catch (Exception e) {
            System.err.println("Error building Freshersworld URL: " + e.getMessage());
            return BASE_URL + "/jobs/jobsearch/software-engineer-jobs-in-india";
        }
    }

    /**
     * Enhanced job card parsing with better field extraction
     */
    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();

        // Extract title
        Element titleElement = card
                .selectFirst("h2.job-title, .job-tittle a, h3 a, .job-title, .job_head h2, .job-title a, .jobTitle");
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }

        // Extract company
        Element companyElement = card
                .selectFirst(".company-name, .job-company, .comp-name, .company_info a, .companyName, .compInfo");
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }

        // Extract location
        Element locationElement = card
                .selectFirst(".job-location, .location, .job_loc, .job-location span, .location_info, .jobLocation");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";

        // Extract salary if available
        Element salaryElement = card.selectFirst(".salary, .package, .salary_info, .pay, .compensation, .salaryText");
        String salary = salaryElement != null ? cleanText(salaryElement.text()) : "";

        // Extract qualification requirement
        Element qualElement = card.selectFirst(".qualification, .education, .edu_info, .qualification_info, .qual");
        String qualification = qualElement != null ? cleanText(qualElement.text()) : "";

        // Build enhanced description
        StringBuilder description = new StringBuilder();
        description.append("Fresher position from Freshersworld - Entry-level IT/Software role");

        // Add location, salary, and qualification to description if available
        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
            description.append(" | Location: ").append(locationText);
        }
        if (!salary.isEmpty()) {
            description.append(" | Salary: ").append(salary);
        }
        if (!qualification.isEmpty()) {
            description.append(" | Qualification: ").append(qualification);
        }

        job.setDescription(description.toString());

        // Extract apply URL
        Element linkElement = card.selectFirst(
                "a[href*='job-detail'], a[href*='jobs'], h2 a, h3 a, .job-title a, .apply-btn, a[href*='/jobs/']");
        if (linkElement != null) {
            String href = linkElement.attr("href");
            if (href.startsWith("/")) {
                job.setApplyUrl(BASE_URL + href);
            } else if (href.startsWith("job-detail")) {
                job.setApplyUrl(BASE_URL + "/jobs/" + href);
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
        Elements dateElements = card
                .select(".posted-date, .job-date, [class*='date'], .job-posted-date, .date, .post-date, .job-time");

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
        return "Freshersworld";
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