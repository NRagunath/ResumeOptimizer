package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for Enhanced Job Scrapers.
 * Tests scraping logic, parsing, and advanced processing pipelines.
 */
class JobScraperVerificationTest {

    private EnhancedLinkedInScraper linkedInScraper;
    private EnhancedIndeedScraper indeedScraper;
    
    // Mocks
    private AdvancedScrapingService mockAdvancedScrapingService;
    private JobLinkVerificationService mockLinkVerificationService;
    private JobDateFilterService mockDateFilterService;
    private AdvancedJobScraperService mockDeepScraperService;

    @BeforeEach
    void setUp() {
        // Initialize scrapers
        linkedInScraper = new EnhancedLinkedInScraper();
        indeedScraper = new EnhancedIndeedScraper();
        
        // Initialize mocks
        mockAdvancedScrapingService = new MockAdvancedScrapingService();
        mockLinkVerificationService = new MockJobLinkVerificationService();
        mockDateFilterService = new MockJobDateFilterService();
        mockDeepScraperService = new MockAdvancedJobScraperService();
        
        // Inject mocks using ReflectionTestUtils (Spring utility) or standard reflection
        injectField(linkedInScraper, "advancedScrapingService", mockAdvancedScrapingService);
        injectField(linkedInScraper, "linkVerificationService", mockLinkVerificationService);
        injectField(linkedInScraper, "dateFilterService", mockDateFilterService);
        injectField(linkedInScraper, "deepScraperService", mockDeepScraperService);
        injectField(linkedInScraper, "enabled", true);
        injectField(linkedInScraper, "searchQuery", "software engineer");
        injectField(linkedInScraper, "location", "India");
        injectField(linkedInScraper, "deepScrapingEnabled", false); // Disable for simple test
        injectField(linkedInScraper, "linkVerificationEnabled", false);
        injectField(linkedInScraper, "maxRetries", 1);
        
        injectField(indeedScraper, "advancedScrapingService", mockAdvancedScrapingService);
        injectField(indeedScraper, "linkVerificationService", mockLinkVerificationService);
        injectField(indeedScraper, "dateFilterService", mockDateFilterService);
        injectField(indeedScraper, "deepScraperService", mockDeepScraperService);
        injectField(indeedScraper, "enabled", true);
        injectField(indeedScraper, "maxRetries", 1);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Try superclass if not found
            try {
                Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                System.err.println("Could not inject field " + fieldName + ": " + ex.getMessage());
            }
        }
    }

    @Test
    void testLinkedInScraper_ShouldParseJobsCorrectly() throws IOException {
        // Setup mock HTML response
        String html = """
            <html>
            <body>
                <ul class="jobs-search__results-list">
                    <li class="job-search-card">
                        <div class="base-search-card__info">
                            <h3 class="base-search-card__title">Junior Java Developer</h3>
                            <h4 class="base-search-card__subtitle">Tech Company</h4>
                            <div class="base-search-card__metadata">
                                <span class="job-search-card__location">Bangalore, India</span>
                                <time class="job-search-card__listdate">2 days ago</time>
                            </div>
                        </div>
                        <a class="base-card__full-link" href="https://linkedin.com/jobs/view/123">Apply</a>
                    </li>
                </ul>
            </body>
            </html>
        """;
        
        ((MockAdvancedScrapingService) mockAdvancedScrapingService).setMockResponse(html);
        
        List<JobListing> jobs = linkedInScraper.scrapeJobs();
        
        assertNotNull(jobs);
        assertEquals(1, jobs.size());
        JobListing job = jobs.get(0);
        assertEquals("Junior Java Developer", job.getTitle());
        assertEquals("Tech Company", job.getCompany());
        assertEquals("Bangalore, India", job.getLocation()); // This might be part of description in some scrapers, but let's check
        assertTrue(job.getApplyUrl().contains("linkedin.com/jobs/view/123"));
    }

    @Test
    void testScraper_ShouldHandleEmptyResults() throws IOException {
        ((MockAdvancedScrapingService) mockAdvancedScrapingService).setMockResponse("<html><body></body></html>");
        
        List<JobListing> jobs = linkedInScraper.scrapeJobs();
        
        assertNotNull(jobs);
        assertTrue(jobs.isEmpty());
    }

    // Mock implementations
    
    static class MockAdvancedScrapingService extends AdvancedScrapingService {
        private String mockHtml = "";
        
        public void setMockResponse(String html) {
            this.mockHtml = html;
        }
        
        @Override
        public Document fetchDocument(String url) throws IOException {
            return Jsoup.parse(mockHtml);
        }
    }
    
    static class MockJobLinkVerificationService extends JobLinkVerificationService {
        @Override
        public List<JobListing> verifyJobLinks(List<JobListing> jobs) {
            return jobs; // Return as is
        }
    }
    
    static class MockJobDateFilterService extends JobDateFilterService {
        @Override
        public List<JobListing> filterByDateRange(List<JobListing> jobs) {
            return jobs; // Return as is
        }
    }
    
    static class MockAdvancedJobScraperService extends AdvancedJobScraperService {
        @Override
        public List<JobListing> enhanceWithDeepScraping(List<JobListing> jobs) {
            return jobs; // Return as is
        }
    }
}
