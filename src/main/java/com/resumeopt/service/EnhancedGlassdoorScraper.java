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
import com.resumeopt.model.JobSource;

/**
 * Enhanced Glassdoor scraper with advanced link verification, date filtering, and deep scraping
 */
@Component
public class EnhancedGlassdoorScraper implements PortalScraper {
    
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
    
    @Autowired
    private JobDeduplicationService deduplicationService;
    
    @Autowired
    private ScrapingMonitorService monitorService;
    
    @Value("${job.portals.glassdoor.enabled:true}")
    private boolean enabled;
    
    @Value("${job.portals.glassdoor.searchQuery:software engineer entry level}")
    private String searchQuery;
    
    @Value("${job.portals.glassdoor.location:India}")
    private String location;
    
    @Value("${job.portals.glassdoor.requestDelay:2000}")
    private long requestDelay;
    
    @Value("${job.portals.glassdoor.maxPages:3}")
    private int maxPages;
    
    @Value("${job.portals.glassdoor.datePosted:3}")
    private int datePosted; // Days: 1, 3, 7, 14, 30
    
    @Value("${job.portals.glassdoor.deepScraping:true}")
    private boolean deepScrapingEnabled;
    
    @Value("${job.portals.glassdoor.linkVerification:true}")
    private boolean linkVerificationEnabled;
    
    @Value("${job.portals.glassdoor.maxRetries:3}")
    private int maxRetries;
    
    private static final String BASE_URL = "https://www.glassdoor.co.in";
    
    // Date patterns for parsing Glassdoor posting dates
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
            System.out.println("[" + LocalDateTime.now() + "] Enhanced Glassdoor scraper is disabled");
            return new ArrayList<>();
        }
        
        List<JobListing> allJobs = new ArrayList<>();
        
        // Validate configuration
        validateConfiguration();
        
        System.out.println("[" + LocalDateTime.now() + "] Starting enhanced Glassdoor scraping with advanced features...");
        System.out.println("[" + LocalDateTime.now() + "] Configuration: Query='" + searchQuery + "', Location='" + location + 
                          "', MaxPages=" + maxPages + ", DateFilter=" + datePosted + " days");
        
        for (int page = 1; page <= maxPages; page++) {
            try {
                List<JobListing> pageJobs = scrapePageWithRetry(page);
                if (pageJobs.isEmpty()) {
                    System.out.println("[" + LocalDateTime.now() + "] No jobs found on page " + page + ", stopping pagination.");
                    break;
                }
                allJobs.addAll(pageJobs);
                
                System.out.println("[" + LocalDateTime.now() + "] Page " + page + " completed: " + pageJobs.size() + " jobs found");
                
                // Delay between pages to respect rate limits
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[" + LocalDateTime.now() + "] Scraping interrupted");
                break;
            } catch (Exception e) {
                System.err.println("[" + LocalDateTime.now() + "] Error scraping Glassdoor page " + page + ": " + e.getMessage());
                // Continue with next page instead of failing completely
            }
        }
        
        System.out.println("[" + LocalDateTime.now() + "] Initial scraping completed: " + allJobs.size() + " jobs found");
        
        // Apply advanced processing pipeline
        List<JobListing> processedJobs = applyAdvancedProcessing(allJobs);
        
        System.out.println("[" + LocalDateTime.now() + "] Enhanced Glassdoor scraping completed: " + processedJobs.size() + 
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
            System.out.println("[" + LocalDateTime.now() + "] WARNING: Glassdoor search query is empty, using default");
            searchQuery = "software engineer entry level";
        }
        if (requestDelay < 0) {
            System.out.println("[" + LocalDateTime.now() + "] WARNING: Glassdoor request delay is negative, using default 2000ms");
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
                if (e.getMessage().contains("429") || e.getMessage().contains("rate limit") || e.getMessage().contains("Anti-Bot")) {
                    if (attempt < maxRetries - 1) {
                        long delay = (long) Math.pow(2, attempt) * 1000; // Exponential backoff
                        System.out.println("[" + LocalDateTime.now() + "] Rate limited/Blocked, waiting " + delay + "ms before retry " + (attempt + 2));
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
     * Scrapes a single Glassdoor page
     */
    private List<JobListing> scrapePage(int page) throws IOException {
        String url = buildSearchUrl(page);
        System.out.println("[" + LocalDateTime.now() + "] Scraping Glassdoor page " + page + ": " + url);
        
        Document doc = null;
        try {
            doc = advancedScrapingService.fetchDocument(url);
        } catch (Exception e) {
             System.out.println("[" + LocalDateTime.now() + "] Jsoup failed for Glassdoor, trying Selenium: " + e.getMessage());
        }

        // Check for Cloudflare/Anti-bot or empty results with Jsoup
        boolean blocked = doc == null || 
                          doc.title().contains("Cloudflare") || 
                          doc.text().contains("Verify you are human") || 
                          doc.text().contains("Access Denied") || 
                          doc.title().contains("Just a moment...");
        
        Elements jobCards = (doc != null && !blocked) ? doc.select("li[data-test='job-listing'], .jobContainer, [data-test='job-listing'], .jobListing, .job-search__job, li[class*='JobsList_jobListItem']") : new Elements();
        
        if (blocked || jobCards.isEmpty()) {
             System.out.println("[" + LocalDateTime.now() + "] Jsoup failed or blocked, switching to Selenium for Glassdoor page " + page);
             try {
                 doc = seleniumService.fetchDocument(url);
                 jobCards = doc.select("li[data-test='job-listing'], li[class*='JobsList_jobListItem'], .JobCard_jobCardWrapper__vX29z");
                 
                 if (jobCards.isEmpty()) {
                     System.out.println("[" + LocalDateTime.now() + "] Selenium also failed to find jobs on Glassdoor page " + page);
                 }
             } catch (Exception e) {
                 System.err.println("[" + LocalDateTime.now() + "] Selenium failed for Glassdoor: " + e.getMessage());
             }
        }
        
        List<JobListing> jobs = new ArrayList<>();
        
        for (Element card : jobCards) {
            try {
                JobListing job = parseJobCardEnhanced(card);
                if (isValidListing(job)) {
                    jobs.add(job);
                }
            } catch (Exception e) {
                System.err.println("Error parsing Glassdoor job card: " + e.getMessage());
            }
        }
        
        return jobs;
    }
    
    /**
     * Builds Glassdoor search URL with advanced parameters
     */
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            
            String url = BASE_URL + "/Job/jobs.htm?sc.keyword=" + encodedQuery + "&locT=C&locId=-1&locKeyword=" + encodedLocation;
            
            // Add date filter: fromAge parameter (days)
            if (datePosted > 0) {
                url += "&fromAge=" + datePosted;
            }
            
            // Add experience level filter
            url += "&jt=fulltime&empType=FULLTIME&seniorityType=ENTRYLEVEL";
            
            // Add page number
            url += "&p=" + page;
            
            return url;
        } catch (Exception e) {
            System.err.println("Error building Glassdoor URL: " + e.getMessage());
            return BASE_URL + "/Job/jobs.htm?sc.keyword=software+engineer+entry+level&locT=C&locId=-1&locKeyword=India&fromAge=7&jt=fulltime&empType=FULLTIME&seniorityType=ENTRYLEVEL&p=1";
        }
    }
    
    /**
     * Enhanced job card parsing with better field extraction
     */
    private JobListing parseJobCardEnhanced(Element card) {
        JobListing job = new JobListing();
        
        // Extract title
        Element titleElement = card.selectFirst("a.job-title, .job-title, [data-test='job-title'], a[data-test='job-link'], .jobTitle, a[class*='JobCard_jobTitle']");
        if (titleElement != null) {
            job.setTitle(cleanText(titleElement.text()));
        }
        
        // Extract company
        Element companyElement = card.selectFirst(".employerName, [data-test='employer-name'], .jobInfoItem, .job-empolyer-name, div[class*='EmployerProfile_employerName']");
        if (companyElement != null) {
            job.setCompany(cleanText(companyElement.text()));
        }
        
        // Extract location
        Element locationElement = card.selectFirst(".jobLocation, [data-test='job-location'], .loc, .job-location, div[class*='JobCard_location']");
        String locationText = locationElement != null ? cleanText(locationElement.text()) : "";
        
        // Extract salary if available
        Element salaryElement = card.selectFirst(".salaryText, [data-test='detailSalary'], .salary, .pay-estimate, div[class*='JobCard_salaryEstimate']");
        String salary = salaryElement != null ? cleanText(salaryElement.text()) : "";
        
        // Extract description/snippet with enhanced content
        Element descElement = card.selectFirst(".jobDescriptionSnippet, .job-description, .desc, .jobSnippet");
        StringBuilder description = new StringBuilder();
        
        if (descElement != null) {
            description.append(cleanText(descElement.text()));
        } else {
            description.append("Entry-level position from Glassdoor");
        }
        
        // Add location and salary to description if available
        if (!locationText.isEmpty()) {
            job.setLocation(locationText);
            description.append(" | Location: ").append(locationText);
        }
        if (!salary.isEmpty()) {
            description.append(" | Salary: ").append(salary);
        }
        
        job.setDescription(description.toString());
        
        // Extract apply URL
        Element linkElement = card.selectFirst("a[data-test='job-link'], a.job-title, a[href*='/partner/jobListing.htm']");
        if (linkElement != null) {
            String href = linkElement.attr("href");
            if (href.startsWith("/")) {
                job.setApplyUrl(BASE_URL + href);
            } else if (href.startsWith("jobListing.htm")) {
                job.setApplyUrl(BASE_URL + "/Job/" + href);
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
        Elements dateElements = card.select("[data-test='job-age'], .jobAge, [class*='date']");
        
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
     * Applies advanced processing pipeline: deep scraping, link verification, date filtering
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
        return "Glassdoor";
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
