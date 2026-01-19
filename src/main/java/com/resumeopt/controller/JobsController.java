package com.resumeopt.controller;

import com.resumeopt.model.JobListing;
import com.resumeopt.service.JobPortalScraperService;
import com.resumeopt.service.ReliableJobDataService;
import com.resumeopt.service.AdvancedJobScraperService;
import com.resumeopt.service.JobDateFilterService;
import com.resumeopt.service.JobSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/jobs")
public class JobsController {

    @Autowired
    private JobPortalScraperService jobPortalScraperService;

    @Autowired
    private ReliableJobDataService reliableJobDataService;

    @Autowired
    private AdvancedJobScraperService advancedJobScraperService;

    @Autowired
    private JobDateFilterService jobDateFilterService;

    @Autowired
    private JobSourceService jobSourceService;

    /**
     * Main jobs page showing real job listings from all 11 portals with pagination
     */
    @GetMapping(produces = "text/html")
    public String showJobs(Model model, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            // Use JobSourceService which has fallback mechanism with sample data
            // For main page, use aggregateAllListings to show broader range of jobs
            // Optimized: Fetch once, filter in memory
            long startTime = System.currentTimeMillis();
            List<JobListing> allJobs = jobSourceService.aggregateAllListings();

            // Get fresh jobs (last 24 hours)
            List<JobListing> freshJobs = allJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusDays(1).isBefore(job.getPostedDate()))
                    .toList();

            // Get weekly jobs
            List<JobListing> weeklyJobs = allJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusWeeks(1).isBefore(job.getPostedDate()))
                    .toList();

            long fetchTime = System.currentTimeMillis() - startTime;
            System.out.println("Jobs loaded in " + fetchTime + "ms");
            model.addAttribute("loadTimeMs", fetchTime);

            // Calculate pagination
            int totalJobs = allJobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = allJobs.subList(startIndex, endIndex);

            model.addAttribute("allJobs", pagedJobs);
            model.addAttribute("freshJobs", freshJobs);
            model.addAttribute("weeklyJobs", weeklyJobs);
            model.addAttribute("totalJobs", totalJobs);
            model.addAttribute("freshJobsCount", freshJobs.size());
            model.addAttribute("weeklyJobsCount", weeklyJobs.size());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("pageSize", size);

            return "jobs/index";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load jobs: " + e.getMessage());
            e.printStackTrace(); // Log the actual error for debugging
            return "jobs/error";
        }
    }

    @GetMapping(produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getJobsAsJson(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            List<JobListing> allJobs = jobSourceService.aggregateAllListings();

            // Calculate pagination
            int totalJobs = allJobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = allJobs.subList(startIndex, endIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", pagedJobs);
            response.put("count", pagedJobs.size());
            response.put("total", totalJobs);
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("pageSize", size);
            response.put("message",
                    "Successfully fetched " + pagedJobs.size() + " of " + totalJobs + " job listings from all sources");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobs", List.of());
            errorResponse.put("count", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Portal jobs page showing job listings from all 11 portals with filtering
     */
    @GetMapping("/portals")
    public String showPortalJobs(
            Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String portal,
            @RequestParam(required = false) String keyword) {
        try {
            // Use JobSourceService which has fallback mechanism with sample data
            List<JobListing> allJobs = jobSourceService.aggregateAllListings();

            // Apply portal filter if specified
            if (portal != null && !portal.isBlank() && !portal.equalsIgnoreCase("all")) {
                allJobs = allJobs.stream()
                        .filter(job -> job.getSource() != null &&
                                job.getSource().name().equalsIgnoreCase(portal))
                        .toList();
            }

            // Apply keyword search if specified
            if (keyword != null && !keyword.isBlank()) {
                String searchTerm = keyword.toLowerCase();
                allJobs = allJobs.stream()
                        .filter(job -> (job.getTitle() != null && job.getTitle().toLowerCase().contains(searchTerm)) ||
                                (job.getDescription() != null
                                        && job.getDescription().toLowerCase().contains(searchTerm))
                                ||
                                (job.getCompany() != null && job.getCompany().toLowerCase().contains(searchTerm)))
                        .toList();
            }

            // Group jobs by portal for statistics (use all jobs before filtering)
            Map<String, List<JobListing>> jobsByPortal = new HashMap<>();
            List<JobListing> originalJobs = jobSourceService.aggregateAllListings();
            for (JobListing job : originalJobs) {
                String source = job.getSource() != null ? job.getSource().name() : "UNKNOWN";
                jobsByPortal.computeIfAbsent(source, k -> new ArrayList<>()).add(job);
            }

            // Get fresh jobs (last 24 hours)
            List<JobListing> freshJobs = allJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusDays(1).isBefore(job.getPostedDate()))
                    .toList();

            // Get weekly jobs
            List<JobListing> weeklyJobs = allJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusWeeks(1).isBefore(job.getPostedDate()))
                    .toList();

            // Calculate pagination
            int totalJobs = allJobs.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalJobs / size));
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = totalJobs > 0 ? allJobs.subList(startIndex, endIndex) : List.of();

            model.addAttribute("allJobs", pagedJobs);
            model.addAttribute("jobsByPortal", jobsByPortal);
            model.addAttribute("portalCount", jobsByPortal.size());
            model.addAttribute("totalJobs", totalJobs);
            model.addAttribute("freshJobsCount", freshJobs.size());
            model.addAttribute("weeklyJobsCount", weeklyJobs.size());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("pageSize", size);
            model.addAttribute("selectedPortal", portal != null ? portal : "all");
            model.addAttribute("searchKeyword", keyword != null ? keyword : "");

            return "jobs/portals";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load portal jobs: " + e.getMessage());
            e.printStackTrace();
            return "jobs/error";
        }
    }

    /**
     * API endpoint to get raw job listings from all 11 portal scrapers without
     * personalization
     */
    @GetMapping(value = "/api/portal-jobs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRawPortalJobs() {
        try {
            // Get all jobs from the 11 portal scrapers directly
            List<JobListing> allJobs = jobPortalScraperService.aggregateFromPortals();

            // Group jobs by source/portal
            Map<String, List<JobListing>> jobsByPortal = new HashMap<>();
            for (JobListing job : allJobs) {
                String source = job.getSource() != null ? job.getSource().name() : "UNKNOWN";
                jobsByPortal.computeIfAbsent(source, k -> new ArrayList<>()).add(job);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", allJobs);
            response.put("count", allJobs.size());
            response.put("jobsByPortal", jobsByPortal);
            response.put("portalCount", jobsByPortal.size());
            response.put("message", "Raw job listings from all portal scrapers without personalization");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobs", List.of());
            errorResponse.put("count", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * API endpoint to get jobs from a specific portal
     */
    @GetMapping(value = "/api/portal-jobs/{portalName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getJobsFromSpecificPortal(@PathVariable String portalName) {
        try {
            List<JobListing> jobs = jobPortalScraperService.scrapeFromPortal(portalName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", jobs);
            response.put("count", jobs.size());
            response.put("portalName", portalName);
            response.put("message", "Job listings from " + portalName + " portal");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobs", List.of());
            errorResponse.put("count", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Show fresher jobs page with job listings from portal scrapers
     */
    @GetMapping({ "/fresher", "/freshers" })
    public String showFresherJobs(Model model, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            // Get jobs directly from portal scrapers without personalization
            List<JobListing> allJobs = jobPortalScraperService.aggregateFromPortals();

            // Filter for entry-level/fresher positions
            List<JobListing> fresherJobs = allJobs.stream()
                    .filter(job -> {
                        // Check experience requirements
                        Integer expReq = job.getExperienceRequired();
                        if (expReq != null) {
                            return expReq <= 2; // Entry-level means 0-2 years
                        }

                        // Check job title for entry-level keywords
                        String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                        return title.contains("fresher") || title.contains("entry") || title.contains("junior") ||
                                title.contains("trainee") || title.contains("intern") || title.contains("new grad");
                    })
                    .toList();

            // Get fresh fresher jobs (last 24 hours)
            List<JobListing> freshFresherJobs = fresherJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusDays(1).isBefore(job.getPostedDate()))
                    .toList();

            // Get weekly fresher jobs
            List<JobListing> weeklyFresherJobs = fresherJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusWeeks(1).isBefore(job.getPostedDate()))
                    .toList();

            // Calculate pagination
            int totalJobs = fresherJobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = fresherJobs.subList(startIndex, endIndex);

            model.addAttribute("allJobs", pagedJobs);
            model.addAttribute("freshJobs", freshFresherJobs);
            model.addAttribute("weeklyJobs", weeklyFresherJobs);
            model.addAttribute("totalJobs", totalJobs);
            model.addAttribute("freshJobsCount", freshFresherJobs.size());
            model.addAttribute("weeklyJobsCount", weeklyFresherJobs.size());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("pageSize", size);

            return "jobs/fresher";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load fresher jobs: " + e.getMessage());
            e.printStackTrace();
            return "jobs/error";
        }
    }

    /**
     * API endpoint to get fresher jobs from portal scrapers
     */
    @GetMapping(value = { "/api/freshers", "/api/fresher" })
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFresherJobsApi(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            // Get jobs directly from portal scrapers without personalization
            List<JobListing> allJobs = jobPortalScraperService.aggregateFromPortals();

            // Filter for entry-level/fresher positions
            List<JobListing> fresherJobs = allJobs.stream()
                    .filter(job -> {
                        // Check experience requirements
                        Integer expReq = job.getExperienceRequired();
                        if (expReq != null) {
                            return expReq <= 2; // Entry-level means 0-2 years
                        }

                        // Check job title for entry-level keywords
                        String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                        return title.contains("fresher") || title.contains("entry") || title.contains("junior") ||
                                title.contains("trainee") || title.contains("intern") || title.contains("new grad");
                    })
                    .toList();

            // Calculate pagination
            int totalJobs = fresherJobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = fresherJobs.subList(startIndex, endIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", pagedJobs);
            response.put("count", pagedJobs.size());
            response.put("total", totalJobs);
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("pageSize", size);
            response.put("message", "Fresher jobs from all portal scrapers without personalization");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobs", List.of());
            errorResponse.put("count", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Show personalized job recommendations page with pagination
     */
    @GetMapping("/recommendations")
    public String showRecommendations(Model model, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            // Use JobSourceService which has fallback mechanism with sample data
            List<JobListing> allJobs = jobSourceService.aggregateAllListings();

            // Calculate pagination for all entry-level jobs
            int totalJobs = allJobs.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalJobs / size));
            int startIndex = Math.max(0, (page - 1) * size);
            int endIndex = Math.min(startIndex + size, totalJobs);

            // Ensure indices are valid
            if (startIndex >= totalJobs) {
                startIndex = Math.max(0, totalJobs - size);
                endIndex = totalJobs;
            }

            List<JobListing> pagedJobs = allJobs.subList(startIndex, endIndex);

            // Get fresh jobs (last 24 hours) and weekly jobs for statistics
            List<JobListing> freshJobs = allJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusDays(1).isBefore(job.getPostedDate()))
                    .toList();
            List<JobListing> weeklyJobs = allJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusWeeks(1).isBefore(job.getPostedDate()))
                    .toList();

            model.addAttribute("allJobs", pagedJobs); // Display paged entry-level jobs in the main list
            model.addAttribute("freshJobs", freshJobs);
            model.addAttribute("weeklyJobs", weeklyJobs);
            model.addAttribute("totalJobs", totalJobs);
            model.addAttribute("freshJobsCount", freshJobs.size());
            model.addAttribute("weeklyJobsCount", weeklyJobs.size());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("pageSize", size);

            return "jobs/recommendations"; // Use the recommendations template
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load job recommendations: " + e.getMessage());
            e.printStackTrace(); // Log the actual error for debugging
            return "jobs/error";
        }
    }

    /**
     * API endpoint to get entry-level job recommendations
     */
    @GetMapping("/api/recommendations")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEntryLevelRecommendationsApi(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "30") int size) {
        try {
            List<JobListing> allJobs = jobSourceService.aggregateAllListings();

            // Filter for entry-level jobs (0-2 years experience)
            List<JobListing> entryLevelJobs = allJobs.stream()
                    .filter(job -> {
                        // Check experience requirements
                        Integer expReq = job.getExperienceRequired();
                        if (expReq != null) {
                            return expReq <= 2; // Entry-level means 0-2 years
                        }

                        // Check job title for entry-level keywords
                        String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                        return title.contains("fresher") || title.contains("entry") || title.contains("junior") ||
                                title.contains("trainee") || title.contains("intern") || title.contains("new grad");
                    })
                    .map(job -> {
                        // Calculate fresher-friendly score based on various factors
                        int fresherScore = calculateFresherFriendlyScore(job);
                        job.setFresherFriendlyScore(fresherScore);
                        return job;
                    })
                    .toList();

            // Calculate pagination
            int totalJobs = entryLevelJobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = entryLevelJobs.subList(startIndex, endIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", pagedJobs);
            response.put("count", pagedJobs.size());
            response.put("total", totalJobs);
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("pageSize", size);
            response.put("message", "Entry-level job recommendations (0-2 years experience)");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobs", List.of());
            errorResponse.put("count", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * API endpoint to get all jobs as JSON with pagination
     */
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllJobsApi(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            List<JobListing> jobs = jobSourceService.aggregateAllListings();

            // Calculate pagination
            int totalJobs = jobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = jobs.subList(startIndex, endIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", pagedJobs);
            response.put("count", pagedJobs.size());
            response.put("total", totalJobs);
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("pageSize", size);
            response.put("message", "Successfully fetched " + pagedJobs.size() + " of " + totalJobs
                    + " real job listings from job portals and reliable data sources");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobs", List.of());
            errorResponse.put("count", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Main API endpoint to get jobs with filtering and pagination options
     */
    @GetMapping("/api/jobs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getJobsApi(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jobTypeFilter, // Experience level filter
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            List<JobListing> jobs = jobSourceService.aggregateAllListings();

            // Apply filters if provided
            if (source != null && !source.isBlank()) {
                jobs = jobs.stream()
                        .filter(job -> job.getSource() != null &&
                                job.getSource().name().equalsIgnoreCase(source))
                        .toList();
            }

            if (jobType != null && !jobType.isBlank()) {
                jobs = jobs.stream()
                        .filter(job -> job.getJobType() != null &&
                                job.getJobType().name().equalsIgnoreCase(jobType))
                        .toList();
            }

            if (keyword != null && !keyword.isBlank()) {
                jobs = jobs.stream()
                        .filter(job -> (job.getTitle() != null
                                && job.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                                (job.getDescription() != null
                                        && job.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                                ||
                                (job.getCompany() != null
                                        && job.getCompany().toLowerCase().contains(keyword.toLowerCase())))
                        .toList();
            }

            // Apply date range filter if specified
            List<JobListing> filteredJobs = jobs; // Initialize with original list
            if (dateRange != null && !dateRange.isBlank()) {
                java.time.LocalDateTime baseDate = java.time.LocalDateTime.now();
                java.time.LocalDateTime cutoffDate;

                switch (dateRange.toLowerCase()) {
                    case "today":
                        cutoffDate = baseDate.minusDays(1);
                        break;
                    case "week":
                        cutoffDate = baseDate.minusWeeks(1);
                        break;
                    case "month":
                        cutoffDate = baseDate.minusMonths(1);
                        break;
                    default:
                        // No date filtering
                        cutoffDate = baseDate.minusYears(10); // Very old date to include all
                        break;
                }

                if (!dateRange.equalsIgnoreCase("all")) {
                    filteredJobs = jobs.stream()
                            .filter(job -> job.getPostedDate() == null || job.getPostedDate().isAfter(cutoffDate))
                            .toList();
                }
            }
            jobs = filteredJobs;

            // Apply experience level filter if specified
            if (jobTypeFilter != null && !jobTypeFilter.isBlank()) {
                if (jobTypeFilter.equalsIgnoreCase("entry-level") ||
                        jobTypeFilter.equalsIgnoreCase("fresher") ||
                        jobTypeFilter.equalsIgnoreCase("new-grad")) {
                    jobs = jobs.stream()
                            .filter(job -> {
                                // Check experience requirements
                                Integer expReq = job.getExperienceRequired();
                                if (expReq != null) {
                                    return expReq <= 2; // Entry-level means 0-2 years
                                }

                                // Check job title for entry-level keywords
                                String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                                return title.contains("fresher") || title.contains("entry") || title.contains("junior")
                                        ||
                                        title.contains("trainee") || title.contains("intern")
                                        || title.contains("new grad");
                            })
                            .toList();
                }
            }

            // Calculate pagination
            int totalJobs = jobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = jobs.subList(startIndex, endIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", pagedJobs);
            response.put("count", pagedJobs.size());
            response.put("total", totalJobs);
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("pageSize", size);
            response.put("message",
                    "Successfully fetched " + pagedJobs.size() + " of " + totalJobs + " job listings from all sources");
            response.put("filters", Map.of(
                    "source", source,
                    "jobType", jobType,
                    "dateRange", dateRange,
                    "keyword", keyword,
                    "experienceLevel", jobTypeFilter // Added experience level filter info
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobs", List.of());
            errorResponse.put("count", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get fresh jobs (last 24 hours) as JSON with pagination
     */
    @GetMapping("/api/fresh")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFreshJobsApi(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            List<JobListing> allJobs = jobSourceService.aggregateAllListings();
            List<JobListing> jobs = allJobs.stream()
                    .filter(job -> job.getPostedDate() != null &&
                            java.time.LocalDateTime.now().minusDays(1).isBefore(job.getPostedDate()))
                    .toList();

            // Calculate pagination
            int totalJobs = jobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = jobs.subList(startIndex, endIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", pagedJobs);
            response.put("count", pagedJobs.size());
            response.put("total", totalJobs);
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("pageSize", size);
            response.put("message", "Fresh jobs from last 24 hours");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get jobs from a specific portal
     */
    @GetMapping("/api/portal/{portalName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getJobsByPortal(@PathVariable String portalName) {
        try {
            List<JobListing> jobs = jobPortalScraperService.scrapeFromPortal(portalName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", jobs);
            response.put("count", jobs.size());
            response.put("portal", portalName);
            response.put("message", "Jobs from " + portalName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("portal", portalName);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Trigger fresh scraping from all portals
     */
    @PostMapping("/api/refresh")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> refreshJobs() {
        try {
            // This will trigger fresh scraping from all portals via JobSourceService
            List<JobListing> jobs = jobSourceService.aggregateAllListings();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", jobs.size());
            response.put("message", "Successfully refreshed job listings from all portals");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get enhanced jobs with deep scraping
     */
    @GetMapping("/api/enhanced")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEnhancedJobs() {
        try {
            // Get basic jobs first via JobSourceService
            List<JobListing> basicJobs = jobSourceService.aggregateAllListings();

            // Enhance with deep scraping
            List<JobListing> enhancedJobs = advancedJobScraperService.enhanceWithDeepScraping(basicJobs);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", enhancedJobs);
            response.put("count", enhancedJobs.size());
            response.put("message", "Enhanced job listings with deep scraping");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get reliable job data (curated and verified)
     */
    @GetMapping("/api/reliable")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReliableJobs() {
        try {
            List<JobListing> jobs = reliableJobDataService.getReliableJobListings();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", jobs);
            response.put("count", jobs.size());
            response.put("message", "Reliable and verified job listings");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Search jobs by keyword
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchJobs(@RequestParam String keyword) {
        try {
            List<JobListing> allJobs = jobSourceService.aggregateAllListings();

            // Filter jobs by keyword
            List<JobListing> filteredJobs = allJobs.stream()
                    .filter(job -> (job.getTitle() != null
                            && job.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                            (job.getDescription() != null
                                    && job.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                            ||
                            (job.getCompany() != null
                                    && job.getCompany().toLowerCase().contains(keyword.toLowerCase())))
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", filteredJobs);
            response.put("count", filteredJobs.size());
            response.put("keyword", keyword);
            response.put("message", "Search results for: " + keyword);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get jobs added since a specific timestamp
     */
    @GetMapping("/api/new-since")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNewJobsSince(@RequestParam long timestamp,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "30") int size) {
        try {
            List<JobListing> allJobs = jobSourceService.aggregateAllListings();

            // Filter jobs added since the given timestamp
            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(timestamp),
                    java.time.ZoneId.systemDefault());

            List<JobListing> newJobs = allJobs.stream()
                    .filter(job -> job.getCreatedAt() != null && job.getCreatedAt().isAfter(cutoffDate))
                    .sorted((j1, j2) -> j2.getCreatedAt().compareTo(j1.getCreatedAt())) // Sort by newest first
                    .toList();

            // Calculate pagination
            int totalJobs = newJobs.size();
            int totalPages = (int) Math.ceil((double) totalJobs / size);
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalJobs);

            List<JobListing> pagedJobs = newJobs.subList(startIndex, endIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", pagedJobs);
            response.put("count", pagedJobs.size());
            response.put("total", totalJobs);
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("pageSize", size);
            response.put("message", "New jobs added since the specified time");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("jobs", List.of());
            errorResponse.put("count", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Calculate fresher-friendly score for a job listing
     * Score is from 1-10 based on various factors
     */
    private int calculateFresherFriendlyScore(JobListing job) {
        int score = 0;

        // Base score for entry-level indicators
        if (job.getExperienceRequired() != null && job.getExperienceRequired() <= 2) {
            score += 4; // High score for 0-2 years experience
        }

        // Check job title for fresher-friendly keywords
        String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
        if (title.contains("fresher") || title.contains("entry") || title.contains("junior") ||
                title.contains("trainee") || title.contains("new grad") || title.contains("intern")) {
            score += 3;
        }

        // Check for remote work (attractive for freshers)
        String description = job.getDescription() != null ? job.getDescription().toLowerCase() : "";
        if (description.contains("remote") || description.contains("work from home")) {
            score += 1;
        }

        // Check for training/professional development
        if (description.contains("training") || description.contains("mentor") ||
                description.contains("learning") || description.contains("development")) {
            score += 2;
        }

        // Cap the score at 10
        return Math.min(score, 10);
    }
}
