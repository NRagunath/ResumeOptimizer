package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import com.resumeopt.model.JobSource;
import com.resumeopt.model.JobType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JobSourceService {

    // Note: For demo purposes, this fetches from public boards where allowed.
    // Real integrations should respect robots.txt and terms of service.

    @Autowired
    private JobFilterService jobFilterService;

    @Autowired
    private LinkVerifierService linkVerifierService;

    @Autowired
    private JobPortalScraperService jobPortalScraperService;

    @Autowired
    private ReliableJobDataService reliableJobDataService;

    @Autowired
    private JobDateFilterService jobDateFilterService;

    @Autowired
    private JobTypeClassificationService jobTypeClassificationService;

    @Autowired(required = false)
    private com.resumeopt.service.SmartDuplicateDetectionService smartDuplicateDetectionService;

    @Autowired(required = false)
    private com.resumeopt.service.AdvancedDataExtractionService advancedDataExtractionService;

    @Value("${job.sources.lever:}")
    private String leverSources;

    /**
     * Aggregate entries from multiple sources, apply strict role-based filter, and verify application links.
     * Caches the result to improve performance.
     */

    @org.springframework.cache.annotation.CacheEvict(
        value = "jobListings",
        allEntries = true
    )
    public void clearCache() {
        System.out.println("Clearing job listings cache");
    }

    /**
     * Removes duplicate jobs based on title, company, and apply URL
     * Also truncates descriptions to fit database constraints
     */
    private List<JobListing> deduplicateJobs(List<JobListing> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return jobs;
        }

        List<JobListing> unique = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();
        int duplicatesRemoved = 0;
        int truncatedCount = 0;

        for (JobListing job : jobs) {
            // Truncate description if too long (max 5000 chars)
            if (
                job.getDescription() != null &&
                job.getDescription().length() > 5000
            ) {
                job.setDescription(
                    job.getDescription().substring(0, 4997) + "..."
                );
                truncatedCount++;
            }

            // Truncate title if too long (max 255 chars, typical default)
            if (job.getTitle() != null && job.getTitle().length() > 255) {
                job.setTitle(job.getTitle().substring(0, 252) + "...");
            }

            // Truncate company if too long (max 255 chars)
            if (job.getCompany() != null && job.getCompany().length() > 255) {
                job.setCompany(job.getCompany().substring(0, 252) + "...");
            }

            // Truncate applyUrl if too long (max 1000 chars)
            if (
                job.getApplyUrl() != null && job.getApplyUrl().length() > 1000
            ) {
                job.setApplyUrl(job.getApplyUrl().substring(0, 997) + "...");
            }

            String hash = generateJobHash(job);
            if (!seenHashes.contains(hash)) {
                seenHashes.add(hash);
                unique.add(job);
            } else {
                duplicatesRemoved++;
            }
        }

        if (duplicatesRemoved > 0) {
            System.out.println(
                "Removed " +
                    duplicatesRemoved +
                    " duplicate jobs across all sources"
            );
        }
        if (truncatedCount > 0) {
            System.out.println(
                "Truncated " +
                    truncatedCount +
                    " job descriptions that exceeded length limit"
            );
        }

        return unique;
    }

    /**
     * Generates a hash for duplicate detection
     */
    private String generateJobHash(JobListing job) {
        String title = job.getTitle() != null ? job.getTitle() : "";
        String company = job.getCompany() != null ? job.getCompany() : "";
        String url = job.getApplyUrl() != null ? job.getApplyUrl() : "";

        String combined = (title + company + url).toLowerCase()
            .replaceAll("\\s+", "")
            .replaceAll("[^a-z0-9]", "");
        return combined;
    }

    /**
     * Aggregates jobs from job portals only
     */
    public List<JobListing> aggregateFromJobPortals() {
        List<JobListing> aggregated = new ArrayList<>();

        try {
            List<JobListing> portalJobs =
                jobPortalScraperService.aggregateFromPortals();
            if (portalJobs != null) {
                for (JobListing job : portalJobs) {
                    job.setSource(JobSource.JOB_PORTAL);
                    if (job.getJobType() == null) {
                        job.setJobType(
                            jobTypeClassificationService.classifyJob(job)
                        );
                    }
                }
                aggregated.addAll(portalJobs);
            }
        } catch (Exception e) {
            System.err.println(
                "Error aggregating from job portals: " + e.getMessage()
            );
        }

        return aggregated;
    }

    /**
     * Aggregates jobs from all sources with filtering options
     */
    public List<JobListing> aggregateAllSources(
        String sourceFilter,
        String jobTypeFilter,
        String dateRange
    ) {
        List<JobListing> aggregated = new ArrayList<>();

        // Only include job portal sources
        boolean includePortals =
            "both".equalsIgnoreCase(sourceFilter) ||
            "portal".equalsIgnoreCase(sourceFilter);

        if (includePortals) {
            aggregated.addAll(aggregateFromJobPortals());
        }

        // Add reliable job data
        try {
            List<JobListing> reliableJobs =
                reliableJobDataService.getFreshersJobs();
            if (reliableJobs != null && !reliableJobs.isEmpty()) {
                for (JobListing job : reliableJobs) {
                    if (job.getSource() == null) {
                        job.setSource(JobSource.JOB_PORTAL); // Default for reliable data
                    }
                    if (job.getJobType() == null) {
                        job.setJobType(
                            jobTypeClassificationService.classifyJob(job)
                        );
                    }
                }
                aggregated.addAll(reliableJobs);
                System.out.println(
                    "Added " +
                        reliableJobs.size() +
                        " jobs from reliable data source to aggregate"
                );
            } else {
                System.out.println(
                    "No reliable jobs found to add to aggregate"
                );
            }
        } catch (Exception e) {
            System.err.println("Reliable job data failed: " + e.getMessage());
        }

        // Enrich with dates
        jobDateFilterService.enrichWithPostedDates(aggregated);

        // Apply date filter
        if (dateRange != null && !dateRange.isBlank()) {
            aggregated = jobDateFilterService.filterByDateRange(
                aggregated,
                dateRange
            );
        } else {
            aggregated = jobDateFilterService.filterByDateRange(aggregated);
        }

        // Apply job type filter
        if (
            jobTypeFilter != null &&
            !jobTypeFilter.isBlank() &&
            !"all".equalsIgnoreCase(jobTypeFilter)
        ) {
            aggregated = filterByJobType(aggregated, jobTypeFilter);
        }

        // Apply role filter (but skip strict fresher filter)
        aggregated = jobFilterService.filterRelevant(aggregated);

        // Advanced data extraction
        if (advancedDataExtractionService != null) {
            for (JobListing job : aggregated) {
                advancedDataExtractionService.enrichJobListing(job);
            }
        }

        // Deduplicate using smart duplicate detection if available
        if (smartDuplicateDetectionService != null) {
            aggregated = smartDuplicateDetectionService.removeDuplicates(
                aggregated
            );
        } else {
            aggregated = deduplicateJobs(aggregated);
        }

        return aggregated;
    }

    /**
     * Filters jobs by job type
     */
    private List<JobListing> filterByJobType(
        List<JobListing> jobs,
        String jobTypeFilter
    ) {
        if (jobs == null || jobs.isEmpty()) {
            return jobs;
        }

        return jobs
            .stream()
            .filter(job -> {
                if (job.getJobType() == null) {
                    return false;
                }

                if ("internship".equalsIgnoreCase(jobTypeFilter)) {
                    return (
                        job.getJobType() == JobType.INTERNSHIP ||
                        job.getJobType() == JobType.BOTH
                    );
                } else if (
                    "fulltime".equalsIgnoreCase(jobTypeFilter) ||
                    "full-time".equalsIgnoreCase(jobTypeFilter)
                ) {
                    return (
                        job.getJobType() == JobType.FULL_TIME ||
                        job.getJobType() == JobType.BOTH
                    );
                }

                return true;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Aggregate entries from multiple sources without strict fresher filter.
     * This method aggregates from all sources but applies less restrictive filtering.
     * Caches the result to improve performance.
     */
    @org.springframework.cache.annotation.Cacheable(
        value = "jobListings",
        key = "'all'"
    )
    public List<JobListing> aggregateAllListings() {
        return aggregateAllListings(null);
    }

    /**
     * Aggregate entries from configured sources and optional user-provided source URL without strict fresher filter.
     */
    public List<JobListing> aggregateAllListings(String sourceUrl) {
        List<JobListing> aggregated = new ArrayList<>();

        System.out.println("Starting all job aggregation...");

        // Add reliable curated job data first
        try {
            List<JobListing> reliableJobs =
                reliableJobDataService.getReliableJobListings(); // Get all reliable jobs, not just fresher
            if (reliableJobs != null && !reliableJobs.isEmpty()) {
                aggregated.addAll(reliableJobs);
                System.out.println(
                    "Added " +
                        reliableJobs.size() +
                        " jobs from reliable data source"
                );
            } else {
                System.out.println("No jobs found from reliable data source");
            }
        } catch (Exception e) {
            System.err.println("Reliable job data failed: " + e.getMessage());
        }

        // Try to scrape from job portals (11 portals)
        try {
            long startTime = System.currentTimeMillis();
            List<JobListing> portalJobs =
                jobPortalScraperService.aggregateFromPortals();
            long elapsed = System.currentTimeMillis() - startTime;
            if (portalJobs != null && !portalJobs.isEmpty()) {
                aggregated.addAll(portalJobs);
                System.out.println(
                    "Added " +
                        portalJobs.size() +
                        " jobs from portal scrapers in " +
                        elapsed +
                        "ms"
                );
            }
        } catch (Exception e) {
            System.err.println("Portal scraping failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Enrich jobs with posted dates if missing
        jobDateFilterService.enrichWithPostedDates(aggregated);

        // Apply basic role filter
        aggregated = jobFilterService.filterRelevant(aggregated);

        // Note: Link verification is now handled within jobPortalScraperService or asynchronously to improve performance

        // Advanced data extraction
        if (advancedDataExtractionService != null) {
            for (JobListing job : aggregated) {
                advancedDataExtractionService.enrichJobListing(job);
            }
        }

        // Deduplicate
        if (smartDuplicateDetectionService != null) {
            aggregated = smartDuplicateDetectionService.removeDuplicates(
                aggregated
            );
        } else {
            aggregated = deduplicateJobs(aggregated);
        }

        System.out.println(
            "Returning " +
                aggregated.size() +
                " job listings from " +
                (aggregated.size() > 0 ? "11 job portals" : "no jobs found")
        );

        return aggregated;
    }
}
