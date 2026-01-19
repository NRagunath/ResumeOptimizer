package com.resumeopt.service;

import java.io.IOException;
import java.util.List;

import com.resumeopt.model.JobListing;

/**
 * Interface for job portal scrapers.
 * Each portal implementation handles its specific HTML structure and scraping logic.
 */
public interface PortalScraper {
    
    /**
     * Scrapes job listings from the portal using configured search parameters
     * @return List of JobListing objects
     * @throws IOException if network or parsing errors occur
     */
    List<JobListing> scrapeJobs() throws IOException;
    
    /**
     * Returns the portal name for logging and identification
     * @return Portal name (e.g., "Indeed", "LinkedIn", "Naukri")
     */
    String getPortalName();
    
    /**
     * Checks if this scraper is enabled in configuration
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();
    
    /**
     * Gets the configured delay between requests in milliseconds
     * @return Delay in milliseconds
     */
    long getRequestDelay();
}
