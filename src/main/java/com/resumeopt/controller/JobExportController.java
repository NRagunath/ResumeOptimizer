package com.resumeopt.controller;

import com.resumeopt.model.JobListing;
import com.resumeopt.service.AdvancedExportService;
import com.resumeopt.service.JobSourceService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for exporting job listings to multiple formats
 */
@Controller
@RequestMapping("/export")
public class JobExportController {

    @Autowired
    private JobSourceService jobSourceService;

    @Autowired(required = false)
    private AdvancedExportService advancedExportService;

    @GetMapping("/jobs/csv")
    public void exportJobsToCSV(HttpServletResponse response)
        throws IOException {
        // Set response headers for CSV download
        response.setContentType("text/csv");
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"job_listings_" +
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
                ) +
                ".csv\""
        );

        // Get job listings
        List<JobListing> jobs = jobSourceService.aggregateAllListings();

        // Use advanced export service if available
        if (advancedExportService != null) {
            byte[] csvData = advancedExportService.exportToCSV(jobs);
            response.getOutputStream().write(csvData);
            response.getOutputStream().flush();
            return;
        }

        // Fallback to basic CSV export
        try (PrintWriter writer = response.getWriter()) {
            // CSV Header
            writer.println(
                "Job Title,Company Name,Location,Job Type,Source,Date Posted,Deadline,Salary,Application Link,Link Verified,Description"
            );

            // CSV Data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd"
            );
            for (JobListing job : jobs) {
                writer.printf(
                    "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    escapeCSV(job.getTitle()),
                    escapeCSV(job.getCompany()),
                    escapeCSV(
                        job.getLocation() != null ? job.getLocation() : "India"
                    ),
                    job.getJobType() != null
                        ? job.getJobType().name()
                        : "FULL_TIME",
                    job.getSource() != null
                        ? job.getSource().name()
                        : "JOB_PORTAL",
                    job.getPostedDate() != null
                        ? job.getPostedDate().format(formatter)
                        : "",
                    job.getApplicationDeadline() != null
                        ? job.getApplicationDeadline().format(formatter)
                        : "",
                    escapeCSV(job.getSalaryRange()),
                    escapeCSV(job.getApplyUrl()),
                    job.getLinkVerified() != null && job.getLinkVerified()
                        ? "Yes"
                        : "No",
                    escapeCSV(job.getDescription())
                );
            }
        }
    }

    /**
     * Export jobs to JSON format
     */
    @GetMapping("/jobs/json")
    public ResponseEntity<String> exportJobsToJSON() throws IOException {
        List<JobListing> jobs = jobSourceService.aggregateAllListings();

        String json;
        if (advancedExportService != null) {
            json = advancedExportService.exportToJSON(jobs);
        } else {
            // Basic JSON export
            json = "{\"jobs\": []}";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"job_listings_" +
                    LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
                    ) +
                    ".json\""
            )
            .body(json);
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and handle null values
        return value.replace("\"", "\"\"");
    }
}
