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
 * Scraper for Freshersworld job portal (India-focused job portal)
 * Disabled in favor of EnhancedFreshersworldScraper
 */
// @Component
public class FreshersworldScraper implements PortalScraper {
    
    @Value("${job.portals.freshersworld.enabled:true}")
    private boolean enabled;
    
    @Value("${job.portals.freshersworld.searchQuery:software engineer}")
    private String searchQuery;
    
    @Value("${job.portals.freshersworld.location:India}")
    private String location;
    
    @Value("${job.portals.freshersworld.requestDelay:2000}")
    private long requestDelay;
    
    @Value("${job.portals.freshersworld.maxPages:3}")
    private int maxPages;
    
    private static final String BASE_URL = "https://www.freshersworld.com";
    
    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        List<JobListing> jobs = new ArrayList<>();
        
        if (searchQuery == null || searchQuery.isBlank()) {
            searchQuery = "software engineer";
        }
        if (requestDelay < 0) {
            requestDelay = 2000;
        }
        if (maxPages < 1) {
            maxPages = 1;
        }
        
        for (int page = 1; page <= maxPages; page++) {
            try {
                String url = buildSearchUrl(page);
                System.out.println("Scraping Freshersworld page " + page + ": " + url);
                
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();
                
                Elements jobCards = doc.select(".job-container, .job-tittle, article.job");
                
                if (jobCards.isEmpty()) {
                    System.out.println("No job cards found on Freshersworld page " + page);
                    break;
                }
                
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseJobCard(card);
                        if (job != null && isValidListing(job)) {
                            jobs.add(job);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing Freshersworld job card: " + e.getMessage());
                    }
                }
                
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("Error fetching Freshersworld page " + page + ": " + e.getMessage());
                throw e;
            }
        }
        
        return jobs;
    }
    
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            // Mark as entry-level
            return BASE_URL + "/jobs/jobsearch/" + encodedQuery.replace("+", "-") + "-jobs-in-india?page=" + page;
        } catch (Exception e) {
            return BASE_URL + "/jobs/jobsearch/software-engineer-jobs-in-india?page=" + page;
        }
    }
    
    private JobListing parseJobCard(Element card) {
        JobListing job = new JobListing();
        
        Element titleElement = card.selectFirst("h2.job-title, .job-tittle a, h3 a");
        if (titleElement != null) {
            job.setTitle(titleElement.text().trim());
        }
        
        Element companyElement = card.selectFirst(".company-name, .job-company");
        if (companyElement != null) {
            job.setCompany(companyElement.text().trim());
        }
        
        job.setDescription("Fresher position from Freshersworld - Entry-level IT/Software role");
        
        Element linkElement = card.selectFirst("a[href*='job-detail'], h2 a, h3 a");
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
        return "Freshersworld";
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
