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
 * Scraper for Internshala job portal (Internships and entry-level jobs)
 * Disabled in favor of EnhancedIntershalaScraper
 */
// @Component
public class IntershalaScraper implements PortalScraper {
    
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
    
    private static final String BASE_URL = "https://internshala.com";
    
    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        List<JobListing> jobs = new ArrayList<>();
        
        if (searchQuery == null || searchQuery.isBlank()) {
            searchQuery = "software development";
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
                System.out.println("Scraping Internshala page " + page + ": " + url);
                
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();
                
                Elements jobCards = doc.select(".individual_internship, .internship_meta");
                
                if (jobCards.isEmpty()) {
                    System.out.println("No job cards found on Internshala page " + page);
                    break;
                }
                
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseJobCard(card);
                        if (job != null && isValidListing(job)) {
                            jobs.add(job);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing Internshala job card: " + e.getMessage());
                    }
                }
                
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("Error fetching Internshala page " + page + ": " + e.getMessage());
                throw e;
            }
        }
        
        return jobs;
    }
    
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            // Internshala has both internships and jobs - we'll search jobs for freshers
            String url = BASE_URL + "/jobs/" + encodedQuery.replace("+", "-") + "-jobs";
            
            // Add fresher filter
            url += "/fresher-job";
            
            // Add page number
            if (page > 1) {
                url += "/page-" + page;
            }
            
            return url;
        } catch (Exception e) {
            return BASE_URL + "/jobs/software-development-jobs/fresher-job";
        }
    }
    
    private JobListing parseJobCard(Element card) {
        JobListing job = new JobListing();
        
        Element titleElement = card.selectFirst(".job-internship-name, .profile a, h3 a");
        if (titleElement != null) {
            job.setTitle(titleElement.text().trim());
        }
        
        Element companyElement = card.selectFirst(".company-name, .company a");
        if (companyElement != null) {
            job.setCompany(companyElement.text().trim());
        }
        
        Element descElement = card.selectFirst(".internship_other_details_container, .job_description");
        if (descElement != null) {
            job.setDescription(descElement.text().trim());
        } else {
            job.setDescription("Entry-level/Fresher position from Internshala");
        }
        
        Element linkElement = card.selectFirst("a.view_detail_button, .profile a");
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
        return "Internshala";
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
