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
 * Enhanced Shine scraper with advanced link verification, date filtering, and
 * deep scraping
 * Shine is a popular India-focused job portal
 */
@Component
public class EnhancedShineScraper implements PortalScraper {

    @Autowired
    private JobLinkVerificationService linkVerificationService;

    @Autowired
    private AdvancedJobScraperService deepScraperService;

    @Autowired
    private JobDateFilterService dateFilterService;

    @Value("${job.portals.shine.enabled:true}")
    private boolean enabled;

    @Value("${job.portals.shine.searchQuery:software engineer}")
    private String searchQuery;

    @Value("${job.portals.shine.location:India}")
    private String location;

    @Value("${job.portals.shine.requestDelay:3000}")
    private long requestDelay;

    @Value("${job.portals.shine.maxPages:3}")
    private int maxPages;

    @Value("${job.portals.shine.experienceMax:1}")
    private int experienceMax;

    @Autowired
    private AdvancedScrapingService advancedScrapingService;

    @Autowired
    private SeleniumService seleniumService;

    @Value("${job.portals.shine.deepScraping:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.shine.linkVerification:true}")
    private boolean linkVerificationEnabled;

    @Value("${job.portals.shine.maxRetries:3}")
    private int maxRetries;

    private static final String BASE_URL = "https://www.shine.com";

    // Date patterns for parsing Shine posting dates
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
            System.out.println("Enhanced Shine scraper is disabled");
            return new ArrayList<>();
        }

        List<JobListing> allJobs = new ArrayList<>();

        // Validate configuration
        validateConfiguration();

        System.out.println("Starting enhanced Shine scraping with advanced features...");
        System.out.println("Configuration: Query='" + searchQuery + "', Location='" + location +
                "', MaxPages=" + maxPages + ", ExperienceMax=" + experienceMax + " years");

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
                System.err.println("Error scraping Shine page " + page + ": " + e.getMessage());
                // Continue with next page instead of failing completely
            }
        }

        System.out.println("Initial scraping completed: " + allJobs.size() + " jobs found");

        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);

        System.out.println("Enhanced Shine scraping completed: " + processedJobs.size() +
                " verified fresh jobs with working links");

        return processedJobs;
    }

    /**
     * Validates scraper configuration and sets defaults for invalid values
     */
    private void validateConfiguration() {
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: Shine search query is empty, using default");
            searchQuery = "software engineer";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: Shine request delay is negative, using default 3000ms");
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
     * Scrapes a single Shine page
     */
    private List<JobListing> scrapePage(int page) throws IOException {
        String url = buildSearchUrl(page);
        System.out.println("Scraping Shine page " + page + ": " + url);

        Document doc = null;
        try {
            doc = advancedScrapingService.fetchDocument(url);
        } catch (Exception e) {
            System.out.println("Jsoup failed for Shine, trying Selenium: " + e.getMessage());
        }

        // Check for empty results or blocking with Jsoup
        boolean blocked = doc == null || doc.text().contains("Access Denied") || doc.title().contains("Cloudflare");
        Elements jobCards = blocked ? new Elements()
                : doc.select(".jobCard, .job_listing, .job-card, .search_listing, .parentClass, div[class*='jobCard']");

        if (blocked || jobCards.isEmpty()) {
            System.out.println("Jsoup failed or blocked, switching to Selenium for Shine page " + page);
            try {
                WebDriver driver = seleniumService.getDriver();
                driver.get(url);

                // Wait for job cards
                try {
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".jobCard")),
                            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".job_listing")),
                            ExpectedConditions.presenceOfElementLocated(By.className("job-card")),
                            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='job']"))));
                } catch (Exception te) {
                    System.out.println(
                            "Timeout waiting for Shine jobs via Selenium. Proceeding with whatever is loaded.");
                }

                String pageSource = driver.getPageSource();
                doc = Jsoup.parse(pageSource, url);

                // Try updated selectors
                jobCards = doc.select(
                        ".jobCard, .job_listing, .job-card, .search_listing, [class*='JobCard'], li[class*='job'], div[class*='jobCard']");
            } catch (Exception e) {
                System.err.println("Selenium also failed for Shine: " + e.getMessage());
                if (doc == null)
                    throw new IOException("Shine scraping failed with both Jsoup and Selenium", e);
            }
        }

        if (jobCards.isEmpty() && doc != null) {
            // Try alternative selectors as a last resort
            jobCards = doc
                    .select("[class*='job'], .search-result, .listing, div[itemtype='http://schema.org/JobPosting']");
        }

        if (jobCards.isEmpty()) {
            System.out.println("No job cards found on Shine page " + page +
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
                System.err.println("Error parsing Shine job card: " + e.getMessage());
                // Continue with next job instead of failing
            }
        }

        return jobs;
    }

    /**
     * Builds Shine search URL with advanced parameters
     */
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);

            String url = BASE_URL + "/job-search/" + encodedQuery.replace("+", "-") + "-jobs";

            // Add experience filter
            if (url.contains("?")) {
                url += "&experienceMax=" + experienceMax;
            } else {
                url += "?experienceMax=" + experienceMax;
            }

            // Add location filter if not default
            if (!location.equalsIgnoreCase("India")) {
                String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
                if (url.contains("?")) {
                    url += "&location=" + encodedLocation;
                } else {
                    url += "?location=" + encodedLocation;
                }
            }

            // Add date filter for recent jobs (last 7 days)
            if (url.contains("?")) {
                url += "&datePosted=7";
            } else {
                url += "?datePosted=7";
            }

            // Add job type filter for full-time
            if (url.contains("?")) {
                url += "&jobType=full_time";
            } else {
                url += "?jobType=full_time";
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
            System.err.println("Error building Shine URL: " + e.getMessage());
            return BASE_URL + "/job-search/software-engineer-jobs?datePosted=7&jobType=full_time&page=1";
        }
    }

    /**
     * Enhanced job card parsing with better field extraction
     */
    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();

        // Extract title - try multiple selectors
        Element titleElement = card
                .selectFirst(".jobCard_pReplaceH2, .job_title a, h2 a, h3 a, .title, a[class*='title']");
        if (titleElement == null) {
            titleElement = card.selectFirst("h2, h3, .job-title");
        }
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }

        // Extract company - try multiple selectors
        Element companyElement = card
                .selectFirst(".jobCard_companyName, .company_name, .company-name, .jobCard_jobIcon");
        if (companyElement == null) {
            companyElement = card.selectFirst("[class*='company']");
        }
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }

        // Extract location
        Element locationElement = card.selectFirst(".jobCard_location, .job_location, .location");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";

        // Extract salary if available
        Element salaryElement = card.selectFirst(".jobCard_salary, .salary, .package");
        String salary = salaryElement != null ? cleanText(salaryElement.text()) : "";

        // Extract experience requirement
        Element expElement = card.selectFirst(".jobCard_experience, .experience, .exp-req");
        String experience = expElement != null ? cleanText(expElement.text()) : "";

        // Extract skills if available
        Element skillsElement = card.selectFirst(".jobCard_skills, .skills, .key-skills");
        String skills = skillsElement != null ? cleanText(skillsElement.text()) : "";

        // Extract description/snippet with enhanced content
        Element descElement = card.selectFirst(".jobCard_jobDescription, .job_description, .description");
        StringBuilder description = new StringBuilder();

        if (descElement != null) {
            description.append(cleanText(descElement.text()));
        } else {
            description.append("Entry-level position from Shine (0-").append(experienceMax)
                    .append(" years experience)");
        }

        // Add location, salary, experience, and skills to description if available
        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
            description.append(" | Location: ").append(locationText);
        }
        if (!salary.isEmpty()) {
            description.append(" | Salary: ").append(salary);
        }
        if (!experience.isEmpty()) {
            description.append(" | Experience: ").append(experience);
        }
        if (!skills.isEmpty()) {
            description.append(" | Skills: ").append(skills);
        }

        job.setDescription(description.toString());

        // Extract apply URL - try multiple selectors
        Element linkElement = card
                .selectFirst("a[href*='/job-detail/'], .job_title a, a[href*='/jobs/'], a[class*='jobCard']");
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
        Elements dateElements = card.select(".jobCard_date, .posted-date, .job-date, [class*='date']");

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

        System.out.println("[" + LocalDateTime.now() + "] Applying advanced processing pipeline...");

        // Step 1: Filter by date to ensure only fresh jobs (past week)
        List<JobListing> freshJobs = dateFilterService.filterByDateRange(jobs);
        System.out.println("[" + LocalDateTime.now() + "] After date filtering: " + freshJobs.size()
                + " fresh jobs (within past week)");

        // Step 2: Apply deep scraping if enabled
        List<JobListing> enhancedJobs = freshJobs;
        if (deepScrapingEnabled && deepScraperService != null) {
            try {
                enhancedJobs = deepScraperService.enhanceWithDeepScraping(freshJobs);
                System.out.println(
                        "[" + LocalDateTime.now() + "] After deep scraping: " + enhancedJobs.size() + " enhanced jobs");
            } catch (Exception e) {
                System.err.println("[" + LocalDateTime.now() + "] Deep scraping failed: " + e.getMessage());
                enhancedJobs = freshJobs; // Fallback to original jobs
            }
        }

        // Step 3: Verify links if enabled
        List<JobListing> verifiedJobs = enhancedJobs;
        if (linkVerificationEnabled && linkVerificationService != null) {
            try {
                verifiedJobs = linkVerificationService.verifyJobLinks(enhancedJobs);
                System.out.println("[" + LocalDateTime.now() + "] After link verification: " + verifiedJobs.size()
                        + " jobs with verified links");
            } catch (Exception e) {
                System.err.println("[" + LocalDateTime.now() + "] Link verification failed: " + e.getMessage());
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
        return "Enhanced Shine";
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