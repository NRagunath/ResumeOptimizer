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
 * Scraper for Jobsora job portal (International job search)
 * Disabled in favor of EnhancedJobsoraScraper
 */
// @Component
public class JobsoraScraper implements PortalScraper {
    
    @Value("${job.portals.jobsora.enabled:true}")
    private boolean enabled;
    
    @Value("${job.portals.jobsora.searchQuery:software engineer entry level}")
    private String searchQuery;
    
    @Value("${job.portals.jobsora.location:India}")
    private String location;
    
    @Value("${job.portals.jobsora.requestDelay:2000}")
    private long requestDelay;
    
    @Value("${job.portals.jobsora.maxPages:3}")
    private int maxPages;
    
    private static final String BASE_URL = "https://in.jobsora.com";
    
    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        List<JobListing> jobs = new ArrayList<>();
        
        if (searchQuery == null || searchQuery.isBlank()) {
            searchQuery = "software engineer entry level";
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
                System.out.println("Scraping Jobsora page " + page + ": " + url);
                
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();
                
                Elements jobCards = doc.select(".vacancy, .job-item");
                
                if (jobCards.isEmpty()) {
                    System.out.println("No job cards found on Jobsora page " + page);
                    break;
                }
                
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseJobCard(card);
                        if (job != null && isValidListing(job)) {
                            jobs.add(job);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing Jobsora job card: " + e.getMessage());
                    }
                }
                
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("Error fetching Jobsora page " + page + ": " + e.getMessage());
                throw e;
            }
        }
        
        return jobs;
    }
    
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            
            String url = BASE_URL + "/search?q=" + encodedQuery + "&l=" + encodedLocation;
            
            // Add page number
            if (page > 1) {
                url += "&p=" + page;
            }
            
            return url;
        } catch (Exception e) {
            return BASE_URL + "/search?q=software+engineer+entry+level&l=India";
        }
    }
    
    private JobListing parseJobCard(Element card) {
        JobListing job = new JobListing();
        
        Element titleElement = card.selectFirst(".vacancy__title, .job-title a, h2 a");
        if (titleElement != null) {
            job.setTitle(titleElement.text().trim());
        }
        
        Element companyElement = card.selectFirst(".vacancy__company, .company-name");
        if (companyElement != null) {
            job.setCompany(companyElement.text().trim());
        }
        
        Element descElement = card.selectFirst(".vacancy__description, .job-description");
        if (descElement != null) {
            job.setDescription(descElement.text().trim());
        } else {
            job.setDescription("Entry-level position from Jobsora");
        }
        
        Element linkElement = card.selectFirst("a.vacancy__title, a[href*='/job/']");
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
        return "Jobsora";
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
