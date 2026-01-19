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
 * Scraper for Glassdoor job portal
 * Disabled in favor of EnhancedGlassdoorScraper
 */
// @Component
public class GlassdoorScraper implements PortalScraper {
    
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
    
    private static final String BASE_URL = "https://www.glassdoor.co.in";
    
    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        List<JobListing> jobs = new ArrayList<>();
        
        if (searchQuery == null || searchQuery.isBlank()) {
            System.out.println("WARNING: Glassdoor search query is empty, using default");
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
                System.out.println("Scraping Glassdoor page " + page + ": " + url);
                
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();
                
                Elements jobCards = doc.select("li[data-test='jobListing'], .react-job-listing");
                
                if (jobCards.isEmpty()) {
                    System.out.println("No job cards found on Glassdoor page " + page);
                    break;
                }
                
                for (Element card : jobCards) {
                    try {
                        JobListing job = parseJobCard(card);
                        if (job != null && isValidListing(job)) {
                            jobs.add(job);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing Glassdoor job card: " + e.getMessage());
                    }
                }
                
                if (page < maxPages && requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("Error fetching Glassdoor page " + page + ": " + e.getMessage());
                throw e;
            }
        }
        
        return jobs;
    }
    
    private String buildSearchUrl(int page) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            
            String url = BASE_URL + "/Job/jobs.htm?sc.keyword=" + encodedQuery + "&locT=N&locId=115&locKeyword=" + encodedLocation;
            
            // Add date filter: fromAge parameter (days)
            if (datePosted > 0) {
                url += "&fromAge=" + datePosted;
            }
            
            // Add experience level filter
            url += "&seniorityType=entrylevel";
            
            // Add page number
            if (page > 1) {
                url += "&p=" + page;
            }
            
            return url;
        } catch (Exception e) {
            return BASE_URL + "/Job/jobs.htm?sc.keyword=software+engineer&locKeyword=India&fromAge=3&seniorityType=entrylevel";
        }
    }
    
    private JobListing parseJobCard(Element card) {
        JobListing job = new JobListing();
        
        Element titleElement = card.selectFirst("a[data-test='job-link'], .jobTitle");
        if (titleElement != null) {
            job.setTitle(titleElement.text().trim());
        }
        
        Element companyElement = card.selectFirst(".employerName, [data-test='employer-name']");
        if (companyElement != null) {
            job.setCompany(companyElement.text().trim());
        }
        
        Element descElement = card.selectFirst(".jobDescriptionContent, .desc");
        if (descElement != null) {
            job.setDescription(descElement.text().trim());
        } else {
            job.setDescription("Entry-level position from Glassdoor");
        }
        
        Element linkElement = card.selectFirst("a[data-test='job-link']");
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
