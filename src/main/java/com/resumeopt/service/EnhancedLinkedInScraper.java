package com.resumeopt.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
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
 * Enhanced LinkedIn scraper with advanced link verification, date filtering,
 * and deep scraping
 * Note: LinkedIn has heavy anti-scraping measures and may require
 * authentication
 */
@Component
public class EnhancedLinkedInScraper implements PortalScraper {

    @Autowired
    private JobLinkVerificationService linkVerificationService;

    @Autowired
    private AdvancedJobScraperService deepScraperService;

    @Autowired
    private JobDateFilterService dateFilterService;

    @Value("${job.portals.linkedin.enabled:true}")
    private boolean enabled;

    @Value("${job.portals.linkedin.searchQuery:entry level software}")
    private String searchQuery;

    @Value("${job.portals.linkedin.location:India}")
    private String location;

    @Value("${job.portals.linkedin.requestDelay:5000}")
    private long requestDelay;

    @Value("${job.portals.linkedin.maxPages:3}")
    private int maxPages;

    @Value("${job.portals.linkedin.requiresAuth:false}")
    private boolean requiresAuth;

    @Value("${job.portals.linkedin.clientId:}")
    private String clientId;

    @Value("${job.portals.linkedin.clientSecret:}")
    private String clientSecret;

    @Autowired
    private AdvancedScrapingService advancedScrapingService;

    @Autowired
    private SeleniumService seleniumService;

    @Value("${job.portals.linkedin.datePosted:r86400}")
    private String datePosted; // r86400 = last 24 hours, r259200 = last 3 days, r604800 = last week

    @Value("${job.portals.linkedin.deepScraping:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.linkedin.linkVerification:true}")
    private boolean linkVerificationEnabled;

    @Value("${job.portals.linkedin.maxRetries:3}")
    private int maxRetries;

    private static final String BASE_URL = "https://www.linkedin.com";

    // Date patterns for parsing LinkedIn posting dates
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
            System.out.println("[" + LocalDateTime.now() + "] Enhanced LinkedIn scraper is disabled");
            return new ArrayList<>();
        }

        List<JobListing> allJobs = new ArrayList<>();

        // Validate configuration
        validateConfiguration();

        System.out
                .println("[" + LocalDateTime.now() + "] Starting enhanced LinkedIn scraping with advanced features...");
        System.out.println(
                "[" + LocalDateTime.now() + "] Configuration: Query='" + searchQuery + "', Location='" + location +
                        "', DateFilter=" + datePosted + ", MaxPages=" + maxPages);

        for (int page = 1; page <= maxPages; page++) {
            try {
                int offset = (page - 1) * 25;
                String url = buildSearchUrl(offset);
                System.out.println("[" + LocalDateTime.now() + "] Scraping LinkedIn page " + page + ": " + url);

                Document doc = null;
                try {
                    doc = advancedScrapingService.fetchDocument(url);
                } catch (Exception e) {
                    System.out.println("Jsoup failed for LinkedIn, trying Selenium: " + e.getMessage());
                }

                // Anti-bot detection
                boolean blocked = doc == null ||
                        doc.title().contains("Security Challenge") ||
                        doc.title().contains("Cloudflare") ||
                        doc.text().contains("authwall") ||
                        doc.text().contains("Sign In to LinkedIn") ||
                        doc.text().contains("Join LinkedIn");

                if (blocked) {
                    System.out.println("[" + LocalDateTime.now()
                            + "] Blocked by LinkedIn anti-bot or authwall with Jsoup. Switching to Selenium...");
                    try {
                        WebDriver driver = seleniumService.getDriver();
                        driver.get(url);

                        // Wait for job cards
                        try {
                            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions
                                            .presenceOfElementLocated(By.cssSelector(".jobs-search__results-list li")),
                                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".job-search-card")),
                                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".base-card")),
                                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("li[class*='job']"))));
                        } catch (Exception te) {
                            System.out.println("Timeout waiting for LinkedIn jobs via Selenium.");
                        }

                        String pageSource = driver.getPageSource();
                        doc = Jsoup.parse(pageSource, url);
                    } catch (Exception e) {
                        System.err.println("Selenium also failed for LinkedIn: " + e.getMessage());
                        if (doc == null)
                            throw new IOException("LinkedIn scraping failed with both Jsoup and Selenium", e);
                    }
                }

                // Check for "No results"
                if (doc != null && (doc.text().contains("No matching jobs found") ||
                        doc.text().contains("No result found") ||
                        doc.text().contains("We couldn't find any jobs"))) {
                    System.out.println("[" + LocalDateTime.now() + "] No jobs found on LinkedIn page " + page);
                    break;
                }

                // LinkedIn uses various selectors depending on page structure
                Elements jobCards = doc != null ? doc.select(
                        ".jobs-search__results-list li, .job-search-card, .base-card, .jobs-search-results__list-item, li[class*='job'], div[class*='job-card'], article[class*='job']")
                        : new Elements();

                if (jobCards.isEmpty()) {
                    System.out.println("[" + LocalDateTime.now() + "] No job cards found on LinkedIn page " + page);
                    if (page == 1) {
                        System.out.println("[" + LocalDateTime.now()
                                + "] Note: LinkedIn has strong anti-scraping measures. Consider using LinkedIn Jobs API for production.");
                    }
                    break;
                }

                System.out.println(
                        "[" + LocalDateTime.now() + "] Found " + jobCards.size() + " job cards on page " + page);

                List<JobListing> pageJobs = new ArrayList<>();
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseJobCardEnhanced(card);
                        if (job != null && isValidListing(job)) {
                            pageJobs.add(job);
                        }
                    } catch (Exception e) {
                        System.err.println(
                                "[" + LocalDateTime.now() + "] Error parsing LinkedIn job card: " + e.getMessage());
                    }
                }
                allJobs.addAll(pageJobs);

                // Delay between pages
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }

            } catch (Exception e) {
                System.err.println(
                        "[" + LocalDateTime.now() + "] Error scraping LinkedIn page " + page + ": " + e.getMessage());
                if (page == 1)
                    break; // Fail fast
            }
        }

        System.out.println(
                "[" + LocalDateTime.now() + "] LinkedIn scraping completed: " + allJobs.size() + " jobs found");

        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);

        System.out
                .println("[" + LocalDateTime.now() + "] Enhanced LinkedIn scraping completed: " + processedJobs.size() +
                        " verified fresh jobs with working links");

        return processedJobs;
    }

    /**
     * Validates scraper configuration and sets defaults for invalid values
     */
    private void validateConfiguration() {
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: LinkedIn search query is empty, using default");
            searchQuery = "entry level software";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: LinkedIn request delay is negative, using default 5000ms");
            requestDelay = 5000;
        }
        if (maxPages < 1)
            maxPages = 1;
    }

    /**
     * Builds LinkedIn search URL with advanced parameters
     */
    private String buildSearchUrl(int offset) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

            String url = BASE_URL + "/jobs/search?keywords=" + encodedQuery + "&location=" + encodedLocation;

            // Add date filter: f_TPR parameter
            if (datePosted != null && !datePosted.isBlank()) {
                url += "&f_TPR=" + datePosted;
            }

            // Add experience level filter: f_E=2 for entry level
            url += "&f_E=2";

            // Add job type filter for full-time: f_JT=F
            url += "&f_JT=F";

            // Add offset
            if (offset > 0) {
                url += "&start=" + offset;
            }

            return url;
        } catch (Exception e) {
            System.err.println("Error building LinkedIn URL: " + e.getMessage());
            return BASE_URL
                    + "/jobs/search?keywords=entry+level+software&location=India&f_TPR=r86400&f_E=2&f_JT=F&start="
                    + offset;
        }
    }

    /**
     * Enhanced job card parsing with better field extraction
     */
    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();

        // Extract title - try multiple selectors
        Element titleElement = card.selectFirst(
                ".job-search-card__title, h3.base-search-card__title, .base-card__title, h3[class*='title'], a[class*='title']");
        if (titleElement == null) {
            titleElement = card.selectFirst("h3, h2, .job-title");
        }
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }

        // Extract company - try multiple selectors
        Element companyElement = card.selectFirst(
                ".job-search-card__company-name, h4.base-search-card__subtitle, .base-card__subtitle, a[class*='company'], span[class*='company']");
        if (companyElement == null) {
            companyElement = card.selectFirst("h4, .company-name");
        }
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }

        // Extract location
        Element locationElement = card.selectFirst(".job-search-card__location, span[class*='location']");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";
        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
        }

        // Extract salary if available
        Element salaryElement = card
                .selectFirst(".job-search-card__salary, .base-search-card__salary, span[class*='salary']");
        String salary = salaryElement != null ? cleanText(salaryElement.text()) : "";

        // Extract description/snippet with enhanced content
        Element descElement = card.selectFirst(".job-search-card__snippet, .base-card__full-description");
        StringBuilder description = new StringBuilder();

        if (descElement != null) {
            description.append(cleanText(descElement.text()));
        } else {
            description.append("Entry-level position from LinkedIn");
        }

        // Add location and salary to description if available
        if (!locationText.isEmpty()) {
            description.append(" | Location: ").append(locationText);
        }
        if (!salary.isEmpty()) {
            description.append(" | Salary: ").append(salary);
        }

        job.setDescription(description.toString());

        // Extract apply URL - try multiple selectors
        Element linkElement = card.selectFirst(
                "a.base-card__full-link, a[href*='/jobs/view/'], a[href*='linkedin.com/jobs'], a[class*='link']");
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
                .select(".job-search-card__listdate, .base-search-card__metadata time, [class*='date']");

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
        return "LinkedIn";
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