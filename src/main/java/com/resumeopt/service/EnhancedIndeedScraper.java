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
 * Enhanced Indeed scraper with advanced scraping capabilities
 */
@Component
public class EnhancedIndeedScraper implements PortalScraper {

    @Autowired
    private JobLinkVerificationService linkVerificationService;

    @Autowired
    private AdvancedJobScraperService deepScraperService;

    @Autowired
    private JobDateFilterService dateFilterService;

    @Value("${job.portals.indeed.enabled:true}")
    private boolean enabled;

    @Value("${job.portals.indeed.searchQuery:software engineer entry level}")
    private String searchQuery;

    @Value("${job.portals.indeed.location:India}")
    private String location;

    @Value("${job.portals.indeed.requestDelay:5000}")
    private long requestDelay;

    @Value("${job.portals.indeed.maxPages:3}")
    private int maxPages;

    @Value("${job.portals.indeed.datePosted:3}")
    private int datePosted; // Days: 1, 3, 7, 14

    @Value("${job.portals.indeed.deepScraping:true}")
    private boolean deepScrapingEnabled;

    @Value("${job.portals.indeed.linkVerification:true}")
    private boolean linkVerificationEnabled;

    @Value("${job.portals.indeed.maxRetries:3}")
    private int maxRetries;

    @Autowired
    private AdvancedScrapingService advancedScrapingService;

    @Autowired
    private SeleniumService seleniumService;

    private static final String BASE_URL = "https://www.indeed.co.in";

    // Date patterns for parsing Indeed posting dates
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
            System.out.println("[" + LocalDateTime.now() + "] Enhanced Indeed scraper is disabled");
            return new ArrayList<>();
        }

        List<JobListing> allJobs = new ArrayList<>();

        // Validate configuration
        if (searchQuery == null || searchQuery.isBlank()) {
            searchQuery = "software engineer entry level";
        }

        System.out.println("[" + LocalDateTime.now() + "] Starting enhanced Indeed scraping with advanced features...");
        System.out.println(
                "[" + LocalDateTime.now() + "] Configuration: Query='" + searchQuery + "', Location='" + location +
                        "', MaxPages=" + maxPages + ", DateFilter=" + datePosted + " days");

        for (int page = 0; page < maxPages; page++) {
            try {
                String url = buildSearchUrl(page);
                System.out.println(
                        "[" + LocalDateTime.now() + "] Enhanced Indeed scraping page " + (page + 1) + ": " + url);

                Document doc;
                try {
                    // Indeed often blocks Jsoup immediately, so we can try Selenium first or as
                    // fallback
                    // Trying Jsoup first for speed if not blocked
                    doc = advancedScrapingService.fetchDocument(url);

                    // Anti-bot detection
                    if (doc.title().contains("Cloudflare") || doc.text().contains("Verify you are human") ||
                            doc.title().contains("Just a moment...") || doc.text().contains("Access Denied")) {
                        throw new IOException("Indeed Anti-Bot detection");
                    }
                } catch (Exception e) {
                    System.out.println("[" + LocalDateTime.now() + "] Standard scraping failed for Indeed ("
                            + e.getMessage() + "). Switching to Selenium...");
                    try {
                        doc = seleniumService.fetchDocument(url);
                    } catch (Exception se) {
                        System.err.println("Selenium scraping also failed: " + se.getMessage());
                        continue;
                    }
                }

                // Check for "No results"
                if (doc.text().contains("did not match any jobs") || doc.text().contains("No jobs found") ||
                        doc.text().contains("No results found")) {
                    System.out.println("[" + LocalDateTime.now() + "] No jobs found on Indeed page " + (page + 1));
                    break;
                }

                Elements jobCards = doc.select(
                        ".jobsearch-ResultsList .job_seen_beacon, .job_seen_beacon, .job_listing, .jobTitle, .resultContent, [class*='job_seen_beacon'], [class*='jobCard']");

                if (jobCards.isEmpty()) {
                    System.out.println("[" + LocalDateTime.now() + "] No job cards found on Indeed page " + (page + 1)
                            + " (Possible layout change or anti-bot)");
                    // If we found no cards but also no "no results" message, it might be a block or
                    // layout change.
                    // Let's try to wait a bit longer or retry if it's the first page
                    if (page == 0) {
                        System.out.println("Retrying page 1 with Selenium due to empty results...");
                        doc = seleniumService.fetchDocument(url);
                        jobCards = doc.select(
                                ".jobsearch-ResultsList .job_seen_beacon, .job_seen_beacon, .job_listing, .jobTitle, .resultContent, [class*='job_seen_beacon']");
                    }

                    if (jobCards.isEmpty())
                        break;
                }

                List<JobListing> pageJobs = new ArrayList<>();
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseJobCardEnhanced(card);
                        if (job != null && isValidListing(job)) {
                            pageJobs.add(job);
                        }
                    } catch (Exception e) {
                        System.err.println(
                                "[" + LocalDateTime.now() + "] Error parsing Indeed job card: " + e.getMessage());
                    }
                }
                allJobs.addAll(pageJobs);
                System.out.println("[" + LocalDateTime.now() + "] Page " + (page + 1) + " completed: " + pageJobs.size()
                        + " jobs found");

                // Delay between pages with Jitter
                if (page < maxPages - 1 && requestDelay > 0) {
                    long jitter = (long) (Math.random() * 2000); // 0-2000ms jitter
                    Thread.sleep(requestDelay + jitter);
                }

            } catch (Exception e) {
                System.err.println("[" + LocalDateTime.now() + "] Error fetching Indeed page " + (page + 1) + ": "
                        + e.getMessage());
                if (page == 0 && e instanceof IOException)
                    throw (IOException) e;
            }
        }

        System.out
                .println("[" + LocalDateTime.now() + "] Initial scraping completed: " + allJobs.size() + " jobs found");

        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);

        System.out.println("[" + LocalDateTime.now() + "] Enhanced Indeed scraping completed: " + processedJobs.size() +
                " verified fresh jobs with working links");

        return processedJobs;
    }

    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            int start = page * 10;

            String url = BASE_URL + "/jobs?q=" + encodedQuery + "&l=" + encodedLocation + "&start=" + start;

            if (datePosted > 0) {
                url += "&fromage=" + datePosted;
            }

            // Use proper parameter for entry level jobs
            url += "&explvl=ENTRY_LEVEL";

            return url;
        } catch (Exception e) {
            return BASE_URL + "/jobs?q=software+engineer+entry+level&l=India&start=" + (page * 10)
                    + "&fromage=7&explvl=ENTRY_LEVEL";
        }
    }

    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();

        Element titleElement = card.selectFirst("h2.jobTitle a, .jobTitle span, a[id^='job_']");
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }

        Element companyElement = card.selectFirst(".companyName, [data-testid='company-name'], .company");
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }

        Element locationElement = card.selectFirst(".companyLocation, [data-testid='text-location']");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";

        Element descElement = card.selectFirst(".job-snippet, .jobCardShelfContainer, .job-description");
        StringBuilder description = new StringBuilder();
        if (descElement != null) {
            description.append(cleanText(descElement.text()));
        } else {
            description.append("Entry-level position from Indeed");
        }

        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
            description.append(" | Location: ").append(locationText);
        }

        job.setDescription(description.toString());

        Element linkElement = card.selectFirst("h2.jobTitle a, a.jcs-JobTitle");
        if (linkElement != null) {
            String href = linkElement.attr("href");
            if (href.startsWith("/")) {
                job.setApplyUrl(BASE_URL + href);
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

    private LocalDateTime extractPostingDate(Element card) {
        Elements dateElements = card.select(".date, .my-job-date, [data-testid='myJobsStateDate']");

        for (Element elem : dateElements) {
            String dateText = elem.text().trim();
            // Remove "Posted" prefix if present
            dateText = dateText.replace("Posted", "").trim();

            LocalDateTime parsed = parseRelativeDate(dateText);
            if (parsed != null) {
                return parsed;
            }
        }

        // Indeed specific: check for "Active X days ago"
        Element activeDate = card.selectFirst(".date");
        if (activeDate != null) {
            String text = activeDate.text();
            if (text.contains("Active")) {
                LocalDateTime parsed = parseRelativeDate(text.replace("Active", "").trim());
                if (parsed != null)
                    return parsed;
            }
        }

        return LocalDateTime.now().minusDays(1); // Default
    }

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

    private List<JobListing> applyAdvancedProcessing(List<JobListing> jobs) {
        if (jobs.isEmpty()) {
            return jobs;
        }

        System.out.println("Applying advanced processing pipeline...");

        // Step 1: Filter by date
        List<JobListing> freshJobs = dateFilterService.filterByDateRange(jobs);
        System.out.println("After date filtering: " + freshJobs.size() + " fresh jobs");

        // Step 2: Apply deep scraping
        List<JobListing> enhancedJobs = freshJobs;
        if (deepScrapingEnabled && deepScraperService != null) {
            try {
                enhancedJobs = deepScraperService.enhanceWithDeepScraping(freshJobs);
                System.out.println("After deep scraping: " + enhancedJobs.size() + " enhanced jobs");
            } catch (Exception e) {
                System.err.println("Deep scraping failed: " + e.getMessage());
                enhancedJobs = freshJobs;
            }
        }

        // Step 3: Verify links
        List<JobListing> verifiedJobs = enhancedJobs;
        if (linkVerificationEnabled && linkVerificationService != null) {
            try {
                verifiedJobs = linkVerificationService.verifyJobLinks(enhancedJobs);
                System.out.println("After link verification: " + verifiedJobs.size() + " jobs with verified links");
            } catch (Exception e) {
                System.err.println("Link verification failed: " + e.getMessage());
                verifiedJobs = enhancedJobs;
            }
        }

        return verifiedJobs;
    }

    private boolean isValidListing(JobListing job) {
        return job.getTitle() != null && !job.getTitle().isBlank()
                && job.getCompany() != null && !job.getCompany().isBlank()
                && job.getApplyUrl() != null && !job.getApplyUrl().isBlank();
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ").replaceAll("[\\r\\n]+", " ");
    }

    @Override
    public String getPortalName() {
        return "Indeed";
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
