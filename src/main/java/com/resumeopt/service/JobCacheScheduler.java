package com.resumeopt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JobCacheScheduler {

    @Autowired
    private JobPortalScraperService jobPortalScraperService;

    @Autowired
    private JobSourceService jobSourceService;

    @EventListener(ApplicationReadyEvent.class)
    public void initCache() {
        System.out.println("Initializing job cache on startup...");
        refreshCache();
    }

    @Scheduled(fixedRate = 3600000) // 1 hour
    public void refreshCache() {
        System.out.println("Refreshing job cache...");
        try {
            // 1. Refresh underlying portal data
            jobPortalScraperService.clearCache();
            // This triggers the scraping and caches the result in "portalScrape"
            jobPortalScraperService.aggregateFromPortals();
            
            // 2. Clear aggregated caches
            jobSourceService.clearCache(); 
            
            // 3. Pre-warm the specific view caches
            // This ensures users don't wait for post-processing (filtering, regex, deduplication)
            System.out.println("Pre-warming 'all' jobs cache...");
            jobSourceService.aggregateAllListings();
            
            System.out.println("Job cache refreshed and warmed successfully");
        } catch (Exception e) {
            System.err.println("Failed to refresh job cache: " + e.getMessage());
            e.printStackTrace();
        }
    }
}