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
 * Scraper for Naukri job portal (India-focused)
 * Disabled in favor of EnhancedNaukriScraper
 */
// @Component
public class NaukriScraper implements PortalScraper {
    
    @Value("${job.portals.naukri.enabled:false}")
    private boolean enabled;
    
    @Value("${job.portals.naukri.searchQuery:software engineer fresher}")
    private String searchQuery;
    
    @Value("${job.portals.naukri.location:India}")
    private String location;
    
    @Value("${job.portals.naukri.requestDelay:2000}")
    private long requestDelay;
    
    @Value("${job.portals.naukri.maxPages:5}")
    private int maxPages;
    
    @Value("${job.portals.naukri.experienceMin:0}")
    private int experienceMin;
    
    @Value("${job.portals.naukri.experienceMax:2}")
    private int experienceMax;
    
    private static final String BASE_URL = "https://www.naukri.com";
    
    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        List<JobListing> jobs = new ArrayList<>();
        
        // Validate configuration
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: Naukri search query is empty, using default");
            searchQuery = "software engineer fresher";
        }
        if (requestDelay < 0) {
            System.out.println("WARNING: Naukri request delay is negative, using default 2000ms");
            requestDelay = 2000;
        }
        if (maxPages < 1) {
            maxPages = 1;
        }
        
        for (int page = 1; page <= maxPages; page++) {
            try {
                String url = buildSearchUrl(page);
                System.out.println("Scraping Naukri page " + page + ": " + url);
                
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();
                
                Elements jobCards = doc.select(".jobTuple, .srp-jobtuple-wrapper, article.jobTuple");
                
                if (jobCards.isEmpty()) {
                    System.out.println("No job cards found on Naukri page " + page);
                    break;
                }
                
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseJobCard(card);
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
                throw e;
            }
        }
        
        return jobs;
    }
    
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String baseUrl = BASE_URL + "/" + encodedQuery.replace("+", "-") + "-jobs";
            
            baseUrl += "-" + page;
            
            // Naukri doesn't have direct date filter in URL, but we can add qp parameter
            // qp=1 means posted in last 1 day, qp=3 means last 3 days
            baseUrl += "?qp=3"; // Last 3 days
            
            return baseUrl;
        } catch (Exception e) {
            return BASE_URL + "/software-engineer-jobs-" + page + "?qp=3";
        }
    }
    
    private JobListing parseJobCard(Element card) {
        JobListing job = new JobListing();
        
        // Extract title
        Element titleElement = card.selectFirst(".title, .jobTitle, a.title");
        if (titleElement != null) {
            job.setTitle(titleElement.text());
        }
        
        // Extract company
        Element companyElement = card.selectFirst(".companyInfo, .comp-name, a.comp-name");
        if (companyElement != null) {
            job.setCompany(companyElement.text());
        }
        
        // Extract description
        Element descElement = card.selectFirst(".job-description, .desc");
        if (descElement != null) {
            job.setDescription(descElement.text());
        } else {
            job.setDescription("Fresher position from Naukri");
        }
        
        // Extract apply URL
        Element linkElement = card.selectFirst("a.title, a[href*='job-listings']");
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
        
        return job;
    }
    
    private boolean isValidListing(JobListing job) {
        return job.getTitle() != null && !job.getTitle().isBlank()
                && job.getCompany() != null && !job.getCompany().isBlank()
                && job.getApplyUrl() != null && !job.getApplyUrl().isBlank();
    }
    
    @Override
    public String getPortalName() {
        return "Naukri";
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
