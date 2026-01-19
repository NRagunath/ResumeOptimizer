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
 * Enhanced Naukri scraper with better date parsing and link verification
 */
@Component
public class EnhancedNaukriScraper implements PortalScraper {
    
    @Value("${job.portals.naukri.enabled:true}")
    private boolean enabled;
    
    @Value("${job.portals.naukri.searchQuery:software engineer}")
    private String searchQuery;
    
    @Value("${job.portals.naukri.location:India}")
    private String location;
    
    @Value("${job.portals.naukri.requestDelay:3000}")
    private long requestDelay;
    
    @Value("${job.portals.naukri.maxPages:3}")
    private int maxPages;
    
    @Value("${job.portals.naukri.experienceMin:0}")
    private int experienceMin;
    
    @Value("${job.portals.naukri.experienceMax:1}")
    private int experienceMax;

    @Autowired
    private AdvancedScrapingService advancedScrapingService;
    
    @Autowired
    private SeleniumService seleniumService;
    
    private static final String BASE_URL = "https://www.naukri.com";
    
    // Enhanced date patterns for Naukri
    private static final Pattern[] DATE_PATTERNS = {
        Pattern.compile("(\\d{1,2})\\s+(hours?|hrs?)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(days?|d)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(weeks?|w)\\s+ago", Pattern.CASE_INSENSITIVE),
        Pattern.compile("yesterday", Pattern.CASE_INSENSITIVE),
        Pattern.compile("today", Pattern.CASE_INSENSITIVE),
        Pattern.compile("just now", Pattern.CASE_INSENSITIVE),
    };
    
    // Add fallback user agent for Naukri to try mobile view if desktop fails
    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36";

    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        List<JobListing> jobs = new ArrayList<>();
        
        // Validate configuration
        validateConfiguration();
        
        for (int page = 1; page <= maxPages; page++) {
            try {
                String url = buildEnhancedSearchUrl(page);
                System.out.println("[" + LocalDateTime.now() + "] Enhanced Naukri scraping page " + page + ": " + url);
                
                Document doc;
                try {
                     // Try standard scraping first (faster)
                     doc = advancedScrapingService.fetchDocument(url);
                     
                     // Check for "No results" or bot block
                     if (doc.text().contains("No matching jobs found") || 
                         doc.text().contains("No result found") || 
                         doc.title().isEmpty() ||
                         doc.select(".jobTuple, .srp-jobtuple-wrapper, [class*='jobTuple']").isEmpty()) {
                         throw new IOException("Possible bot block or empty result");
                     }
                } catch (Exception e) {
                    System.out.println("[" + LocalDateTime.now() + "] Standard scraping failed for Naukri (" + e.getMessage() + "). Switching to Selenium...");
                    // Fallback to Selenium
                    try {
                        doc = seleniumService.fetchDocument(url);
                    } catch (Exception se) {
                        System.err.println("Selenium scraping also failed: " + se.getMessage());
                        continue;
                    }
                }

                // Check for "No results" message first
                if (doc.text().contains("No matching jobs found") || doc.text().contains("No result found")) {
                    System.out.println("[" + LocalDateTime.now() + "] No jobs found on Naukri page " + page + " (verified by page content)");
                    break;
                }

                // Try multiple selectors for job cards
                Elements jobCards = findJobCards(doc);
                
                if (jobCards.isEmpty()) {
                    System.out.println("[" + LocalDateTime.now() + "] No job cards found on Naukri page " + page + " even with Selenium.");
                } else {
                    System.out.println("[" + LocalDateTime.now() + "] Found " + jobCards.size() + " jobs on page " + page);
                }

                if (jobCards.isEmpty()) {
                    System.out.println("[" + LocalDateTime.now() + "] No job cards found on Naukri page " + page);
                    
                    // Check for potential anti-bot or structure change
                if (doc.title().contains("Security Challenge") || 
                    doc.text().contains("pardon our interruption") || 
                    doc.title().contains("Cloudflare")) {
                     System.err.println("[" + LocalDateTime.now() + "] CRITICAL: Blocked by Naukri anti-bot");
                     throw new IOException("Naukri Anti-Bot detection");
                }
                    
                    if (page == 1) {
                        System.out.println("[" + LocalDateTime.now() + "] WARNING: No jobs found on first page. Site structure may have changed or selectors need update.");
                        // Log a snippet of the page for debugging (first 500 chars)
                        String snippet = doc.body().text();
                        System.out.println("Page snippet: " + (snippet.length() > 500 ? snippet.substring(0, 500) : snippet));
                    }
                    break;
                }
                
                System.out.println("[" + LocalDateTime.now() + "] Found " + jobCards.size() + " job cards on page " + page);
                
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseEnhancedJobCard(card);
                        if (job != null && isValidListing(job)) {
                            jobs.add(job);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing Naukri job card: " + e.getMessage());
                    }
                }
                
                // Delay between pages
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("Error fetching Naukri page " + page + ": " + e.getMessage());
                if (page == 1) {
                    throw e; // Fail fast if first page fails
                }
                break; // Continue with what we have if later pages fail
            }
        }
        
        System.out.println("[" + LocalDateTime.now() + "] Enhanced Naukri scraper found " + jobs.size() + " jobs");
        return jobs;
    }
    
    private void validateConfiguration() {
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("[" + LocalDateTime.now() + "] WARNING: Naukri search query is empty, using default");
            searchQuery = "software engineer";
        }
        if (requestDelay < 0) {
            System.out.println("[" + LocalDateTime.now() + "] WARNING: Naukri request delay is negative, using default 3000ms");
            requestDelay = 3000;
        }
        if (maxPages < 1) {
            maxPages = 1;
        }
    }
    
    private String buildEnhancedSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            
            // Build URL with proper Naukri format - using job search format
            StringBuilder urlBuilder = new StringBuilder(BASE_URL);
            urlBuilder.append("/").append(encodedQuery.replace(" ", "-").toLowerCase()).append("-jobs");
            
            // Add location parameter
            if (location != null && !location.isBlank()) {
                String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
                urlBuilder.append("-").append(location.toLowerCase().replace(" ", "-").replace("&", "and"));
            }
            
            // Add experience range
            if (experienceMin >= 0 && experienceMax > 0) {
                urlBuilder.append("-").append(experienceMin).append("-to-").append(experienceMax).append("-years");
            }
            
            // Add page number
            if (page > 1) {
                urlBuilder.append("-").append(page);
            }
            
            // Add query parameters
            urlBuilder.append("?");
            urlBuilder.append("k=").append(encodedQuery);
            urlBuilder.append("&l=").append(location != null ? location : "");
            urlBuilder.append("&experienceMin=").append(experienceMin);
            urlBuilder.append("&experienceMax=").append(experienceMax);
            
            return urlBuilder.toString();
            
        } catch (Exception e) {
            System.err.println("[" + LocalDateTime.now() + "] Error building Naukri URL: " + e.getMessage());
            // Fallback to a simpler URL format
            return BASE_URL + "/software-engineer-jobs-india";
        }
    }
    
    private Elements findJobCards(Document doc) {
        // Try multiple selectors as Naukri changes their structure frequently
        String[] selectors = {
            ".jobTuple",
            ".srp-jobtuple-wrapper",
            "article.jobTuple",
            ".job-tuple",
            "[class*='jobTuple']",
            ".job-card",
            "[data-job-id]",
            ".job-listing"
        };
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                System.out.println("[" + LocalDateTime.now() + "] Found job cards using selector: " + selector);
                return elements;
            }
        }
        
        return new Elements(); // Return empty if nothing found
    }
    
    private JobListing parseEnhancedJobCard(Element card) {
        JobListing job = new JobListing();
        
        // Extract title with multiple selectors
        String title = extractText(card, new String[]{
            ".title", ".jobTitle", "a.title", ".job-title", 
            "h2", "h3", "[class*='title']", ".position"
        });
        job.setTitle(title);
        
        // Extract company with multiple selectors
        String company = extractText(card, new String[]{
            ".companyInfo", ".comp-name", "a.comp-name", 
            ".company", ".employer", "[class*='company']"
        });
        job.setCompany(company);
        
        // Extract location
        String jobLocation = extractText(card, new String[]{
            ".location", ".job-location", ".locationsContainer",
            "[class*='location']", ".workplace"
        });
        
        // Extract salary
        String salary = extractText(card, new String[]{
            ".salary", ".compensation", ".package", 
            "[class*='salary']", "[class*='package']"
        });
        
        // Extract description
        String description = extractText(card, new String[]{
            ".job-description", ".desc", ".snippet", 
            ".job-summary", "[class*='description']"
        });
        if (description == null || description.isBlank()) {
            if (searchQuery.toLowerCase().contains("entry level")) {
                description = "Software engineer entry level position from Naukri. " + searchQuery;
            } else {
                description = "Software engineering position from Naukri";
            }
        }
        
        // Append location and salary to description if available
        if (jobLocation != null && !jobLocation.isBlank()) {
            job.setLocation(jobLocation);
            description += " | Location: " + jobLocation;
        }
        if (salary != null && !salary.isBlank()) {
            description += " | Salary: " + salary;
        }
        
        job.setDescription(description);
        
        // Extract apply URL with multiple selectors
        String applyUrl = extractHref(card, new String[]{
            "a.title", "a[href*='job-listings']", "a[href*='/jobs/']",
            ".apply-link", ".job-link", "a[class*='title']"
        });
        
        if (applyUrl != null) {
            if (applyUrl.startsWith("/")) {
                job.setApplyUrl(BASE_URL + applyUrl);
            } else if (!applyUrl.startsWith("http")) {
                job.setApplyUrl(BASE_URL + "/" + applyUrl);
            } else {
                job.setApplyUrl(applyUrl);
            }
        }
        
        // Extract and parse posted date
        LocalDateTime postedDate = extractPostedDate(card);
        job.setPostedDate(postedDate);
        
        return job;
    }
    
    private String extractText(Element card, String[] selectors) {
        for (String selector : selectors) {
            Element element = card.selectFirst(selector);
            if (element != null) {
                String text = element.text().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }
    
    private String extractHref(Element card, String[] selectors) {
        for (String selector : selectors) {
            Element element = card.selectFirst(selector);
            if (element != null) {
                String href = element.attr("href");
                if (!href.isEmpty()) {
                    return href;
                }
            }
        }
        return null;
    }
    
    private LocalDateTime extractPostedDate(Element card) {
        // Look for date elements
        String[] dateSelectors = {
            ".date", ".posted-date", ".job-date", ".time-stamp",
            "[class*='date']", "[class*='time']", ".posted"
        };
        
        for (String selector : dateSelectors) {
            Element dateElement = card.selectFirst(selector);
            if (dateElement != null) {
                String dateText = dateElement.text().trim();
                LocalDateTime parsed = parseRelativeDate(dateText);
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        
        // If no specific date element, look in the entire card text
        String cardText = card.text();
        return parseRelativeDate(cardText);
    }
    
    private LocalDateTime parseRelativeDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    if (text.toLowerCase().contains("today") || text.toLowerCase().contains("just now")) {
                        return now;
                    } else if (text.toLowerCase().contains("yesterday")) {
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
    
    private boolean isValidListing(JobListing job) {
        return job.getTitle() != null && !job.getTitle().isBlank()
                && job.getCompany() != null && !job.getCompany().isBlank()
                && job.getApplyUrl() != null && !job.getApplyUrl().isBlank()
                && !job.getApplyUrl().contains("javascript:void(0)"); // Filter out invalid links
    }
    
    @Override
    public String getPortalName() {
        return "Enhanced Naukri";
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