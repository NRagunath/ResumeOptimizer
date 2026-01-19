package com.resumeopt.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.resumeopt.model.JobListing;

/**
 * Scraper for LinkedIn job portal
 * Note: LinkedIn has heavy anti-scraping measures and may require authentication
 * Disabled in favor of EnhancedLinkedInScraper
 */
// @Component
public class LinkedInScraper implements PortalScraper {
    
    @Value("${job.portals.linkedin.enabled:false}")
    private boolean enabled;
    
    @Value("${job.portals.linkedin.searchQuery:entry level software}")
    private String searchQuery;
    
    @Value("${job.portals.linkedin.location:India}")
    private String location;
    
    @Value("${job.portals.linkedin.requestDelay:3000}")
    private long requestDelay;
    
    @Value("${job.portals.linkedin.requiresAuth:true}")
    private boolean requiresAuth;
    
    @Value("${job.portals.linkedin.clientId:}")
    private String clientId;
    
    @Value("${job.portals.linkedin.clientSecret:}")
    private String clientSecret;
    
    @Value("${job.portals.linkedin.datePosted:r86400}")
    private String datePosted; // r86400 = last 24 hours, r259200 = last 3 days, r604800 = last week
    
    private static final String BASE_URL = "https://www.linkedin.com";
    
    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        // Check if authentication credentials are configured
        if (requiresAuth && (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())) {
            System.out.println("LinkedIn scraping requires authentication credentials which are not configured. Skipping.");
            return Collections.emptyList();
        }
        
        // If credentials are provided, we can proceed (auth is configured)
        if (clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank()) {
            System.out.println("LinkedIn API credentials configured. Client ID: " + clientId.substring(0, Math.min(5, clientId.length())) + "...");
            // Note: Full OAuth flow would require user authorization and access token
            // For now, we'll attempt basic scraping with proper headers
        }
        
        List<JobListing> jobs = new ArrayList<>();
        
        // Validate configuration
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: LinkedIn search query is empty, using default");
            searchQuery = "entry level software";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: LinkedIn request delay is negative, using default 3000ms");
            requestDelay = 3000;
        }
        
        try {
            String url = buildSearchUrl();
            System.out.println("Scraping LinkedIn: " + url);
            
            // Use more realistic headers to avoid blocking
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .referrer("https://www.linkedin.com/")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();
            
            // LinkedIn uses various selectors depending on page structure
            // Try multiple selector patterns
            Elements jobCards = doc.select(".jobs-search__results-list li, .job-search-card, .base-card, .jobs-search-results__list-item");
            
            if (jobCards.isEmpty()) {
                // Try alternative selectors for guest/public view
                jobCards = doc.select("li[class*='job'], div[class*='job-card'], article[class*='job']");
            }
            
            if (jobCards.isEmpty()) {
                System.out.println("No job cards found on LinkedIn. Page may require authentication or use dynamic loading.");
                System.out.println("Note: LinkedIn has strong anti-scraping measures. Consider using LinkedIn Jobs API for production.");
                return Collections.emptyList();
            }
            
            System.out.println("Found " + jobCards.size() + " potential job cards on LinkedIn");
            
            for (Element card : jobCards) {
                try {
                    JobListing job = parseJobCard(card);
                    if (job != null && isValidListing(job)) {
                        jobs.add(job);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing LinkedIn job card: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            if (e.getMessage().contains("403") || e.getMessage().contains("401")) {
                System.err.println("LinkedIn authentication required or access forbidden");
                throw new IOException("LinkedIn authentication required");
            }
            System.err.println("Error fetching LinkedIn jobs: " + e.getMessage());
            throw e;
        }
        
        return jobs;
    }
    
    private String buildSearchUrl() {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            
            String url = BASE_URL + "/jobs/search?keywords=" + encodedQuery + "&location=" + encodedLocation;
            
            // Add date filter: f_TPR parameter
            // r86400 = last 24 hours, r259200 = last 3 days, r604800 = last week
            if (datePosted != null && !datePosted.isBlank()) {
                url += "&f_TPR=" + datePosted;
            }
            
            // Add experience level filter: f_E=2 for entry level
            url += "&f_E=2";
            
            // Add job type filter for full-time: f_JT=F
            url += "&f_JT=F";
            
            return url;
        } catch (Exception e) {
            return BASE_URL + "/jobs/search?keywords=entry+level+software&location=India&f_TPR=r86400&f_E=2&f_JT=F";
        }
    }
    
    private JobListing parseJobCard(Element card) {
        JobListing job = new JobListing();
        
        // Extract title - try multiple selectors
        Element titleElement = card.selectFirst(".job-search-card__title, h3.base-search-card__title, .base-card__title, h3[class*='title']");
        if (titleElement == null) {
            titleElement = card.selectFirst("h3, h2, .job-title");
        }
        if (titleElement != null) {
            job.setTitle(titleElement.text().trim());
        }
        
        // Extract company - try multiple selectors
        Element companyElement = card.selectFirst(".job-search-card__company-name, h4.base-search-card__subtitle, .base-card__subtitle, a[class*='company'], span[class*='company']");
        if (companyElement == null) {
            companyElement = card.selectFirst("h4, .company-name");
        }
        if (companyElement != null) {
            job.setCompany(companyElement.text().trim());
        }
        
        // Extract location
        Element locationElement = card.selectFirst(".job-search-card__location, .base-search-card__metadata, span[class*='location']");
        String locationText = locationElement != null ? locationElement.text().trim() : "";
        
        // Extract description/snippet
        Element descElement = card.selectFirst(".job-search-card__snippet, .base-card__full-description");
        if (descElement != null) {
            job.setDescription(descElement.text().trim());
        } else {
            // Build description from available info
            String desc = "Entry-level position from LinkedIn";
            if (!locationText.isEmpty()) {
                desc += " | Location: " + locationText;
            }
            job.setDescription(desc);
        }
        
        // Extract apply URL - try multiple selectors
        Element linkElement = card.selectFirst("a.base-card__full-link, a[href*='/jobs/view/'], a[href*='linkedin.com/jobs']");
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
        
        return job;
    }
    
    private boolean isValidListing(JobListing job) {
        return job.getTitle() != null && !job.getTitle().isBlank()
                && job.getCompany() != null && !job.getCompany().isBlank()
                && job.getApplyUrl() != null && !job.getApplyUrl().isBlank();
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
