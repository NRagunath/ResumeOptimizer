package com.resumeopt.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.resumeopt.model.JobListing;

/**
 * Simple, reliable job scraper that focuses on working sources
 * Disabled as it's not part of the specified 11 portals
 */
// @Component
public class SimpleJobScraper implements PortalScraper {
    
    private boolean enabled = true;
    private long requestDelay = 3000;
    
    @Override
    public List<JobListing> scrapeJobs() throws IOException {
        List<JobListing> jobs = new ArrayList<>();
        
        // Try multiple simple sources
        jobs.addAll(scrapeFromRemoteOK());
        jobs.addAll(scrapeFromStackOverflow());
        
        return jobs;
    }
    
    private List<JobListing> scrapeFromRemoteOK() {
        List<JobListing> jobs = new ArrayList<>();
        try {
            String url = "https://remoteok.io/remote-dev-jobs";
            System.out.println("Scraping RemoteOK: " + url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();
            
            Elements jobCards = doc.select(".job");
            
            for (Element card : jobCards.stream().limit(5).toList()) {
                try {
                    JobListing job = new JobListing();
                    
                    Element titleElement = card.selectFirst(".company_and_position h2");
                    if (titleElement != null) {
                        job.setTitle(titleElement.text());
                    }
                    
                    Element companyElement = card.selectFirst(".company h3");
                    if (companyElement != null) {
                        job.setCompany(companyElement.text());
                    }
                    
                    job.setDescription("Remote software development position - Entry level welcome");
                    
                    String href = card.attr("data-href");
                    if (href != null && !href.isEmpty()) {
                        job.setApplyUrl("https://remoteok.io" + href);
                    }
                    
                    if (isValidListing(job)) {
                        jobs.add(job);
                    }
                } catch (Exception e) {
                    // Skip invalid jobs
                }
            }
            
            Thread.sleep(requestDelay);
            
        } catch (Exception e) {
            System.err.println("RemoteOK scraping failed: " + e.getMessage());
        }
        
        return jobs;
    }
    
    private List<JobListing> scrapeFromStackOverflow() {
        List<JobListing> jobs = new ArrayList<>();
        try {
            String url = "https://stackoverflow.com/jobs?q=software+engineer&l=India";
            System.out.println("Scraping StackOverflow Jobs: " + url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();
            
            Elements jobCards = doc.select(".listResults .job-link");
            
            for (Element card : jobCards.stream().limit(5).toList()) {
                try {
                    JobListing job = new JobListing();
                    
                    Element titleElement = card.selectFirst(".job-link-title");
                    if (titleElement != null) {
                        job.setTitle(titleElement.text());
                    }
                    
                    Element companyElement = card.selectFirst(".job-link-company");
                    if (companyElement != null) {
                        job.setCompany(companyElement.text());
                    }
                    
                    job.setDescription("Software engineering position from StackOverflow Jobs");
                    
                    String href = card.attr("href");
                    if (href != null && !href.isEmpty()) {
                        if (href.startsWith("/")) {
                            job.setApplyUrl("https://stackoverflow.com" + href);
                        } else {
                            job.setApplyUrl(href);
                        }
                    }
                    
                    if (isValidListing(job)) {
                        jobs.add(job);
                    }
                } catch (Exception e) {
                    // Skip invalid jobs
                }
            }
            
        } catch (Exception e) {
            System.err.println("StackOverflow Jobs scraping failed: " + e.getMessage());
        }
        
        return jobs;
    }
    
    private boolean isValidListing(JobListing job) {
        return job.getTitle() != null && !job.getTitle().isBlank()
                && job.getCompany() != null && !job.getCompany().isBlank()
                && job.getApplyUrl() != null && !job.getApplyUrl().isBlank();
    }
    
    @Override
    public String getPortalName() {
        return "SimpleJobScraper";
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