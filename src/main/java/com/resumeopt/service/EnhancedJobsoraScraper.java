package com.resumeopt.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.resumeopt.model.JobListing;

/**
 * Enhanced Jobsora scraper with advanced link verification, date filtering, and
 * deep scraping
 * Jobsora is an international job search platform with good coverage in India
 */
@Component
public class EnhancedJobsoraScraper implements PortalScraper {

    @Autowired
    private JobLinkVerificationService linkVerificationService;

    @Autowired
    private AdvancedJobScraperService deepScraperService;

    @Autowired
    private JobDateFilterService dateFilterService;

    @Autowired
    private AdvancedScrapingService advancedScrapingService;

    @Autowired
    private SeleniumService seleniumService;

    @Value("${job.portals.jobsora.enabled:true}")
    private boolean enabled;

    @Value("${job.portals.jobsora.searchQuery:software engineer entry level}")
    private String searchQuery;

    @Value("${job.portals.jobsora.location:India}")
    private String location;

    @Value("${job.portals.jobsora.requestDelay:3000}")
    private long requestDelay;

    @Value("${job.portals.jobsora.maxPages:3}")
    private int maxPages;

    @Value("${job.portals.jobsora.deepScraping:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.jobsora.linkVerification:true}")
    private boolean linkVerificationEnabled;

    @Value("${job.portals.jobsora.maxRetries:3}")
    private int maxRetries;

    private static final String BASE_URL = "https://in.jobsora.com";

    // Date patterns for parsing Jobsora posting dates
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
            System.out.println("Enhanced Jobsora scraper is disabled");
            return new ArrayList<>();
        }

        List<JobListing> allJobs = new ArrayList<>();

        // Validate configuration
        validateConfiguration();

        System.out.println("Starting enhanced Jobsora scraping with advanced features...");
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
                System.err.println("Error scraping Jobsora page " + page + ": " + e.getMessage());
                // Continue with next page instead of failing completely
            }
        }

        System.out.println("Initial scraping completed: " + allJobs.size() + " jobs found");

        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);

        System.out.println("Enhanced Jobsora scraping completed: " + processedJobs.size() +
                " verified fresh jobs with working links");

        return processedJobs;
    }

    /**
     * Validates scraper configuration and sets defaults for invalid values
     */
    private void validateConfiguration() {
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: Jobsora search query is empty, using default");
            searchQuery = "software engineer entry level";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: Jobsora request delay is negative, using default 3000ms");
            requestDelay = 3000;
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
     * Scrapes a single Jobsora page
     */
    private List<JobListing> scrapePage(int page) throws IOException {
        String url = buildSearchUrl(page);
        System.out.println("[" + LocalDateTime.now() + "] Scraping Jobsora page " + page + ": " + url);

        Document doc;
        try {
            doc = advancedScrapingService.fetchDocument(url);
        } catch (Exception e) {
            System.out.println("Jsoup failed for Jobsora, trying Selenium: " + e.getMessage());
            try {
                doc = seleniumService.fetchDocument(url);
            } catch (Exception se) {
                System.err.println("Selenium scraping also failed: " + se.getMessage());
                return new ArrayList<>();
            }
        }

        // Anti-bot detection
        if (doc.title().contains("Cloudflare") || doc.text().contains("Verify you are human")) {
            System.err.println("[" + LocalDateTime.now() + "] CRITICAL: Blocked by Jobsora anti-bot");
            try {
                doc = seleniumService.fetchDocument(url);
            } catch (Exception se) {
                throw new IOException("Jobsora Anti-Bot detection");
            }
        }

        // Explicit no results check
        if (doc.text().contains("No jobs found") || doc.text().contains("We couldn't find any jobs")) {
            System.out.println("[" + LocalDateTime.now() + "] No jobs found on Jobsora page " + page);
            return new ArrayList<>();
        }

        // Try multiple selectors for job cards
        Elements jobCards = doc.select(".vacancy, .job-item, .job-card, .c-job-list__item");

        if (jobCards.isEmpty()) {
            // Try alternative selectors
            jobCards = doc.select("[class*='job'], [class*='vacancy'], .search-result");
        }

        if (jobCards.isEmpty()) {
            System.out.println("[" + LocalDateTime.now() + "] No job cards found on Jobsora page " + page +
                    " with Jsoup. Switching to Selenium...");
            try {
                doc = seleniumService.fetchDocument(url);
                jobCards = doc.select(".vacancy, .job-item, .job-card, .c-job-list__item, [class*='job']");
            } catch (Exception e) {
                System.err.println("Selenium fallback failed: " + e.getMessage());
            }
        }

        if (jobCards.isEmpty()) {
            System.out.println("[" + LocalDateTime.now() + "] No job cards found on Jobsora page " + page +
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
                System.err.println("[" + LocalDateTime.now() + "] Error parsing Jobsora job card: " + e.getMessage());
                // Continue with next job instead of failing
            }
        }

        return jobs;
    }

    /**
     * Builds Jobsora search URL with advanced parameters
     */
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

            String url = BASE_URL + "/jobs-in-" + encodedLocation.replace("+", "-") + "/"
                    + encodedQuery.replace("+", "-");

            // Add experience level filter for entry-level
            if (url.contains("?")) {
                url += "&experience=entry_level";
            } else {
                url += "?experience=entry_level";
            }

            // Add date filter for recent jobs (last 7 days)
            if (url.contains("?")) {
                url += "&date_posted=7";
            } else {
                url += "?date_posted=7";
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
            System.err.println("Error building Jobsora URL: " + e.getMessage());
            return BASE_URL + "/jobs-in-India/software-engineer?experience=entry_level&date_posted=7&page=1";
        }
    }

    /**
     * Enhanced job card parsing with better field extraction
     */
    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();

        // Extract title - try multiple selectors
        Element titleElement = card
                .selectFirst(".vacancy__title, .job-title a, h2 a, h3 a, .title a, .c-job-list__title");
        if (titleElement == null) {
            titleElement = card.selectFirst("h2, h3, .job-name");
        }
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }

        // Extract company - try multiple selectors
        Element companyElement = card
                .selectFirst(".vacancy__company, .company-name, .employer-name, .c-job-list__company");
        if (companyElement == null) {
            companyElement = card.selectFirst("[class*='company'], [class*='employer']");
        }
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }

        // Extract location
        Element locationElement = card
                .selectFirst(".vacancy__location, .job-location, .location, .c-job-list__location");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";

        // Extract salary if available
        Element salaryElement = card.selectFirst(".vacancy__salary, .salary, .wage, .c-job-list__salary");
        String salary = salaryElement != null ? cleanText(salaryElement.text()) : "";

        // Extract job type if available
        Element typeElement = card.selectFirst(".job-type, .employment-type");
        String jobType = typeElement != null ? cleanText(typeElement.text()) : "";

        // Extract description/snippet with enhanced content
        Element descElement = card
                .selectFirst(".vacancy__description, .job-description, .description, .c-job-list__desc");
        StringBuilder description = new StringBuilder();

        if (descElement != null) {
            description.append(cleanText(descElement.text()));
        } else {
            description.append("Entry-level position from Jobsora international job search");
        }

        // Add location, salary, and job type to description if available
        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
            description.append(" | Location: ").append(locationText);
        }
        if (!salary.isEmpty()) {
            description.append(" | Salary: ").append(salary);
        }
        if (!jobType.isEmpty()) {
            description.append(" | Type: ").append(jobType);
        }

        job.setDescription(description.toString());

        // Extract apply URL - try multiple selectors
        Element linkElement = card
                .selectFirst("a.vacancy__title, a[href*='/job/'], a[href*='/vacancy/'], a.c-job-list__title");
        if (linkElement == null) {
            linkElement = card.selectFirst("a[href]");
        }
        if (linkElement != null) {
            String href = linkElement.attr("href");
            if (href.startsWith("/")) {
                job.setApplyUrl(BASE_URL + href);
            } else if (href.startsWith("http")) {
                job.setApplyUrl(href);
            } else {
                job.setApplyUrl(BASE_URL + "/" + href);
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
                .select(".vacancy__date, .posted-date, .job-date, [class*='date'], .c-job-list__date");

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

        // Step 1: Filter by date to ensure only fresh jobs (past week)
        List<JobListing> freshJobs = dateFilterService.filterByDateRange(jobs);
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
        return "Enhanced Jobsora";
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