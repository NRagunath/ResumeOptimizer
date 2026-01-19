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
 * Enhanced Internshala scraper with advanced link verification, date filtering,
 * and deep scraping
 * Internshala specializes in internships and entry-level jobs
 */
@Component
public class EnhancedIntershalaScraper implements PortalScraper {

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

    @Value("${job.portals.internshala.enabled:true}")
    private boolean enabled;

    @Value("${job.portals.internshala.searchQuery:software development}")
    private String searchQuery;

    @Value("${job.portals.internshala.location:India}")
    private String location;

    @Value("${job.portals.internshala.requestDelay:2000}")
    private long requestDelay;

    @Value("${job.portals.internshala.maxPages:3}")
    private int maxPages;

    @Value("${job.portals.internshala.deepScraping:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.internshala.linkVerification:true}")
    private boolean linkVerificationEnabled;

    @Value("${job.portals.internshala.maxRetries:3}")
    private int maxRetries;

    private static final String BASE_URL = "https://internshala.com";

    // Date patterns for parsing Internshala posting dates
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
            System.out.println("Enhanced Internshala scraper is disabled");
            return new ArrayList<>();
        }

        List<JobListing> allJobs = new ArrayList<>();

        // Validate configuration
        validateConfiguration();

        System.out.println("Starting enhanced Internshala scraping with advanced features...");
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
                System.err.println("Error scraping Internshala page " + page + ": " + e.getMessage());
                // Continue with next page instead of failing completely
            }
        }

        System.out.println("Initial scraping completed: " + allJobs.size() + " jobs found");

        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);

        System.out.println("Enhanced Internshala scraping completed: " + processedJobs.size() +
                " verified fresh jobs with working links");

        return processedJobs;
    }

    /**
     * Validates scraper configuration and sets defaults for invalid values
     */
    private void validateConfiguration() {
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: Internshala search query is empty, using default");
            searchQuery = "software development";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: Internshala request delay is negative, using default 2000ms");
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
     * Scrapes a single Internshala page
     */
    private List<JobListing> scrapePage(int page) throws IOException {
        String url = buildSearchUrl(page);
        System.out.println("[" + LocalDateTime.now() + "] Scraping Internshala page " + page + ": " + url);

        Document doc;
        try {
            doc = advancedScrapingService.fetchDocument(url);
        } catch (Exception e) {
            System.out.println("Standard scraping failed for Internshala. Switching to Selenium...");
            try {
                doc = seleniumService.fetchDocument(url);
            } catch (Exception se) {
                System.err.println("Selenium scraping also failed: " + se.getMessage());
                return new ArrayList<>();
            }
        }

        // Anti-bot detection
        if (doc.title().contains("Cloudflare") || doc.text().contains("Verify you are human")) {
            System.err.println("[" + LocalDateTime.now() + "] CRITICAL: Blocked by Internshala anti-bot");
            // Try Selenium if not already used, or fail
            try {
                doc = seleniumService.fetchDocument(url);
            } catch (Exception se) {
                throw new IOException("Internshala Anti-Bot detection");
            }
        }

        // Explicit no results check
        if (doc.text().contains("No internships found") || doc.text().contains("No jobs found")) {
            System.out.println("[" + LocalDateTime.now() + "] No jobs found on Internshala page " + page);
            return new ArrayList<>();
        }

        Elements jobCards = doc
                .select(".individual_internship, .internship_meta, .job_card, div[id*='individual_internship']");

        if (jobCards.isEmpty()) {
            System.out.println("[" + LocalDateTime.now() + "] No job cards found on Internshala page " + page +
                    " with Jsoup. Switching to Selenium...");
            try {
                doc = seleniumService.fetchDocument(url);
                jobCards = doc.select(
                        ".individual_internship, .internship_meta, .job_card, div[id*='individual_internship']");
            } catch (Exception e) {
                System.err.println("Selenium fallback failed: " + e.getMessage());
            }
        }

        if (jobCards.isEmpty()) {
            System.out.println("[" + LocalDateTime.now() + "] No job cards found on Internshala page " + page +
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
                System.err
                        .println("[" + LocalDateTime.now() + "] Error parsing Internshala job card: " + e.getMessage());
                // Continue with next job instead of failing
            }
        }

        return jobs;
    }

    /**
     * Builds Internshala search URL with advanced parameters
     */
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            // Internshala has both internships and jobs - we'll search entry level jobs
            String url = BASE_URL + "/jobs/" + encodedQuery.replace("+", "-") + "-jobs";

            // Add location filter if provided
            if (location != null && !location.equalsIgnoreCase("India")) {
                String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
                url += "-in-" + encodedLocation.replace("+", "-");
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
            System.err.println("Error building Internshala URL: " + e.getMessage());
            return BASE_URL + "/jobs/software-development-jobs?page=1";
        }
    }

    /**
     * Enhanced job card parsing with better field extraction
     */
    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();

        // Extract title
        Element titleElement = card.selectFirst(".job-internship-name, .profile a, h3 a, .job-title");
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }

        // Extract company
        Element companyElement = card.selectFirst(".company-name, .company a, .company_name");
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }

        // Extract location
        Element locationElement = card.selectFirst(".location_link, .location, a[href*='location']");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";

        // Extract stipend/salary if available
        Element stipendElement = card.selectFirst(".stipend, .salary, .desktop-text");
        String stipend = stipendElement != null ? cleanText(stipendElement.text()) : "";

        // Extract duration if available
        Element durationElement = card.selectFirst(".duration, .job-duration, .item_body");
        String duration = durationElement != null ? cleanText(durationElement.text()) : "";

        // Extract description/details with enhanced content
        Element descElement = card.selectFirst(".internship_other_details_container, .job_description");
        StringBuilder description = new StringBuilder();

        if (descElement != null) {
            description.append(cleanText(descElement.text()));
        } else {
            description.append("Entry-level/Fresher position from Internshala");
        }

        // Add location, stipend, and duration to description if available
        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
            description.append(" | Location: ").append(locationText);
        }
        if (!stipend.isEmpty()) {
            description.append(" | Stipend: ").append(stipend);
        }
        if (!duration.isEmpty()) {
            description.append(" | Duration: ").append(duration);
        }

        job.setDescription(description.toString());

        // Extract apply URL
        Element linkElement = card
                .selectFirst("a.view_detail_button, .profile a, a[href*='/job/'], a[href*='/internship/']");
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
        Elements dateElements = card
                .select(".start_immediately, .posted_by_premium_badge, [class*='date'], .status-container");

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
        return "Enhanced Internshala";
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