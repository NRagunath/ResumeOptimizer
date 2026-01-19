package com.resumeopt.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.resumeopt.model.JobListing;

/**
 * Scraper for Indeed job portal
 * Disabled in favor of EnhancedIndeedScraper
 */
// @Component
public class IndeedScraper implements PortalScraper {
    
    @Value("${job.portals.indeed.enabled:false}")
    private boolean enabled;
    
    @Value("${job.portals.indeed.searchQuery:software engineer entry level}")
    private String searchQuery;
    
    @Value("${job.portals.indeed.location:India}")
    private String location;
    
    @Value("${job.portals.indeed.requestDelay:2000}")
    private long requestDelay;
    
    @Value("${job.portals.indeed.maxPages:3}")
    private int maxPages;
    
    @Value("${job.portals.indeed.datePosted:3}")
    private int datePosted; // Days: 1, 3, 7, 14
    
    private static final String BASE_URL = "https://www.indeed.com";
    
    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        List<JobListing> jobs = new ArrayList<>();
        
        // Validate configuration
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: Indeed search query is empty, using default");
            searchQuery = "software engineer entry level";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: Indeed request delay is negative, using default 2000ms");
            requestDelay = 2000;
        }
        if (maxPages < 1) {
            maxPages = 1;
        }
        
        for (int page = 0; page < maxPages; page++) {
            try {
                String url = buildSearchUrl(page);
                System.out.println("Scraping Indeed page " + (page + 1) + ": " + url);
                
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.5")
                        .header("Accept-Encoding", "gzip, deflate")
                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .timeout(15000)
                        .followRedirects(true)
                        .get();
                
                Elements jobCards = doc.select(".jobsearch-ResultsList .job_seen_beacon, .job_seen_beacon");
                
                if (jobCards.isEmpty()) {
                    System.out.println("No job cards found on Indeed page " + (page + 1));
                    break;
                }
                
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseJobCard(card);
                        if (job != null && isValidListing(job)) {
                            jobs.add(job);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing Indeed job card: " + e.getMessage());
                    }
                }
                
                // Delay between pages
                if (page < maxPages - 1 && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("Error fetching Indeed page " + (page + 1) + ": " + e.getMessage());
                throw e;
            }
        }
        
        return jobs;
    }
    
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            int start = page * 10; // Indeed uses 10 results per page
            
            // Add date filter: fromage parameter (days)
            // 1 = last 24 hours, 3 = last 3 days, 7 = last week, 14 = last 2 weeks
            String url = BASE_URL + "/jobs?q=" + encodedQuery + "&l=" + encodedLocation + "&start=" + start;
            
            if (datePosted > 0) {
                url += "&fromage=" + datePosted;
            }
            
            // Add experience level filter for entry level
            url += "&explvl=entry_level";
            
            return url;
        } catch (Exception e) {
            return BASE_URL + "/jobs?q=software+engineer&l=India&start=" + (page * 10) + "&fromage=3&explvl=entry_level";
        }
    }
    
    private JobListing parseJobCard(Element card) {
        JobListing job = new JobListing();
        
        // Extract title
        Element titleElement = card.selectFirst("h2.jobTitle a, .jobTitle span");
        if (titleElement != null) {
            job.setTitle(titleElement.text());
        }
        
        // Extract company
        Element companyElement = card.selectFirst(".companyName, [data-testid='company-name']");
        if (companyElement != null) {
            job.setCompany(companyElement.text());
        }
        
        // Extract description/snippet
        Element descElement = card.selectFirst(".job-snippet, .jobCardShelfContainer");
        if (descElement != null) {
            job.setDescription(descElement.text());
        } else {
            job.setDescription("Entry-level position from Indeed");
        }
        
        // Extract apply URL
        Element linkElement = card.selectFirst("h2.jobTitle a");
        if (linkElement != null) {
            String href = linkElement.attr("href");
            if (href.startsWith("/")) {
                job.setApplyUrl(BASE_URL + href);
            } else {
                job.setApplyUrl(href);
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
