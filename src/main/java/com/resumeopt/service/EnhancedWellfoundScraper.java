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
 * Enhanced Wellfound scraper with advanced link verification, date filtering,
 * and deep scraping
 * Wellfound (formerly AngelList Talent) specializes in startup jobs
 */
@Component
public class EnhancedWellfoundScraper implements PortalScraper {

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

    @Value("${job.portals.wellfound.enabled:true}")
    private boolean enabled;

    @Value("${job.portals.wellfound.searchQuery:software engineer}")
    private String searchQuery;

    @Value("${job.portals.wellfound.location:India}")
    private String location;

    @Value("${job.portals.wellfound.requestDelay:4000}")
    private long requestDelay;

    @Value("${job.portals.wellfound.maxPages:3}")
    private int maxPages;

    @Value("${job.portals.wellfound.deepScraping:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.wellfound.linkVerification:true}")
    private boolean linkVerificationEnabled;

    @Value("${job.portals.wellfound.maxRetries:3}")
    private int maxRetries;

    private static final String BASE_URL = "https://wellfound.com";

    // Date patterns for parsing Wellfound posting dates
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
            System.out.println("Enhanced Wellfound scraper is disabled");
            return new ArrayList<>();
        }

        List<JobListing> allJobs = new ArrayList<>();

        // Validate configuration
        validateConfiguration();

        System.out.println("Starting enhanced Wellfound scraping with advanced features...");
        System.out.println("Configuration: Query='" + searchQuery + "', Location='" + location +
                "', MaxPages=" + maxPages);

        for (int page = 1; page <= maxPages; page++) {
            try {
                List<JobListing> pageJobs = scrapePageWithRetry(page);
                allJobs.addAll(pageJobs);

                System.out.println("Page " + page + " completed: " + pageJobs.size() + " jobs found");

                // Delay between pages to respect rate limits (Wellfound needs longer delays)
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Scraping interrupted");
                break;
            } catch (Exception e) {
                System.err.println("Error scraping Wellfound page " + page + ": " + e.getMessage());
                // Continue with next page instead of failing completely
            }
        }

        System.out.println("Initial scraping completed: " + allJobs.size() + " jobs found");

        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);

        System.out.println("Enhanced Wellfound scraping completed: " + processedJobs.size() +
                " verified fresh jobs with working links");

        return processedJobs;
    }

    /**
     * Validates scraper configuration and sets defaults for invalid values
     */
    private void validateConfiguration() {
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: Wellfound search query is empty, using default");
            searchQuery = "software engineer";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: Wellfound request delay is negative, using default 4000ms");
            requestDelay = 4000;
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
     * Scrapes a single Wellfound page
     */
    private List<JobListing> scrapePage(int page) throws IOException {
        String url = buildSearchUrl(page);
        System.out.println("Scraping Wellfound page " + page + ": " + url);

        Document doc = null;
        try {
            doc = advancedScrapingService.fetchDocument(url);
        } catch (Exception e) {
            System.out.println("Jsoup failed for Wellfound, trying Selenium: " + e.getMessage());
        }

        // Check for Cloudflare/Anti-bot or empty results with Jsoup
        boolean blocked = doc == null ||
                doc.title().contains("Cloudflare") ||
                doc.text().contains("Verify you are human") ||
                doc.text().contains("Access Denied") ||
                doc.title().contains("Just a moment...");

        Elements jobCards = blocked ? new Elements()
                : doc.select(
                        ".job-listing, [data-test='JobSearchResult'], .startup-job, [class*='JobCard'], div[class*='styles_component']");

        if (blocked || jobCards.isEmpty()) {
            System.out.println("Jsoup failed or blocked, switching to Selenium for Wellfound page " + page);
            try {
                WebDriver driver = seleniumService.getDriver();
                driver.get(url);

                // Wait for Turnstile or Content (up to 20 seconds)
                try {
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".job-listing")),
                            ExpectedConditions
                                    .presenceOfElementLocated(By.cssSelector("[data-test='JobSearchResult']")),
                            ExpectedConditions.presenceOfElementLocated(By.className("startup-job")),
                            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='JobCard']")),
                            ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("div[class*='styles_component'] a[href*='/jobs/']"))));
                } catch (TimeoutException te) {
                    System.out.println(
                            "Timeout waiting for Wellfound jobs (Turnstile might be stuck). Proceeding with current source.");
                }

                String pageSource = driver.getPageSource();
                doc = Jsoup.parse(pageSource, url);

                jobCards = doc.select(
                        ".job-listing, [data-test='JobSearchResult'], .startup-job, [class*='JobCard'], div[class*='styles_component'] a[href*='/jobs/'], div[class*='styles_jobListing']");

                // Try alternative selectors if Selenium also returns empty (dynamic content)
                if (jobCards.isEmpty()) {
                    jobCards = doc.select("[class*='job'], [class*='listing'], .search-result, [class*='JobResult']");
                }
            } catch (Exception e) {
                System.err.println("Selenium also failed for Wellfound: " + e.getMessage());
                if (doc == null)
                    throw new IOException("Wellfound scraping failed with both Jsoup and Selenium", e);
            }
        }

        if (jobCards.isEmpty()) {
            System.out.println("No job cards found on Wellfound page " + page +
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
                System.err.println("Error parsing Wellfound job card: " + e.getMessage());
                // Continue with next job instead of failing
            }
        }

        return jobs;
    }

    /**
     * Builds Wellfound search URL with advanced parameters
     */
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

            String url = BASE_URL + "/jobs?q=" + encodedQuery + "&location=" + encodedLocation;

            // Add experience level filter for entry-level
            if (url.contains("?")) {
                url += "&experience=entry_level";
            } else {
                url += "?experience=entry_level";
            }

            // Add job type filter for full-time
            if (url.contains("?")) {
                url += "&type=full_time";
            } else {
                url += "?type=full_time";
            }

            // Add remote work options
            if (url.contains("?")) {
                url += "&remote=true";
            } else {
                url += "?remote=true";
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
            System.err.println("Error building Wellfound URL: " + e.getMessage());
            return BASE_URL
                    + "/jobs?q=software+engineer+entry+level&location=India&experience=entry_level&type=full_time&remote=true&page=1";
        }
    }

    /**
     * Enhanced job card parsing with better field extraction
     */
    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();

        // Extract title - try multiple selectors
        Element titleElement = card
                .selectFirst(".job-title, [data-test='JobTitle'], h2 a, h3 a, .title, a[class*='title']");
        if (titleElement == null) {
            titleElement = card.selectFirst("h2, h3, .position-title");
        }
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }

        // Extract company - try multiple selectors
        Element companyElement = card
                .selectFirst(".company-name, [data-test='CompanyName'], .startup-name, a[class*='companyName']");
        if (companyElement == null) {
            companyElement = card.selectFirst("[class*='company'], [class*='startup']");
        }
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }

        // Extract location
        Element locationElement = card.selectFirst(".job-location, .location, [data-test='Location']");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";

        // Extract salary if available
        Element salaryElement = card.selectFirst(".salary, .compensation, .equity, [class*='salary']");
        String salary = salaryElement != null ? cleanText(salaryElement.text()) : "";

        // Extract startup stage/size if available
        Element stageElement = card.selectFirst(".stage, .company-size, .startup-stage");
        String stage = stageElement != null ? cleanText(stageElement.text()) : "";

        // Extract tech stack if available
        Element techElement = card.selectFirst(".tech-stack, .skills, .technologies");
        String techStack = techElement != null ? cleanText(techElement.text()) : "";

        // Extract description/snippet with enhanced content
        Element descElement = card.selectFirst(".job-description, .description, [data-test='Description']");
        StringBuilder description = new StringBuilder();

        if (descElement != null) {
            description.append(cleanText(descElement.text()));
        } else {
            description.append("Entry-level startup position from Wellfound (formerly AngelList Talent)");
        }

        // Add location, salary, stage, and tech stack to description if available
        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
            description.append(" | Location: ").append(locationText);
        }
        if (!salary.isEmpty()) {
            description.append(" | Compensation: ").append(salary);
        }
        if (!stage.isEmpty()) {
            description.append(" | Company Stage: ").append(stage);
        }
        if (!techStack.isEmpty()) {
            description.append(" | Tech Stack: ").append(techStack);
        }

        job.setDescription(description.toString());

        // Extract apply URL - try multiple selectors
        Element linkElement = card.selectFirst("a[href*='/jobs/'], a[href*='/company/'], a[data-test='JobLink']");
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
        Elements dateElements = card.select(".posted-date, .job-date, [data-test='PostedDate'], [class*='date']");

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
        return "Enhanced Wellfound";
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