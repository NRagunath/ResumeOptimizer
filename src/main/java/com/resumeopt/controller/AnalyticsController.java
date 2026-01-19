package com.resumeopt.controller;

import com.resumeopt.model.JobListing;
import com.resumeopt.repo.JobListingRepository;
import com.resumeopt.service.JobAnalyticsService;
import com.resumeopt.service.SalaryPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics API controller for market insights and trends
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    
    @Autowired
    private JobAnalyticsService jobAnalyticsService;
    
    @Autowired
    private JobListingRepository jobListingRepository;
    
    @Autowired(required = false)
    private SalaryPredictionService salaryPredictionService;
    
    /**
     * Get market trends analysis
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getMarketTrends() {
        try {
            List<JobListing> allJobs = jobListingRepository.findAll();
            Map<String, Object> trends = jobAnalyticsService.analyzeMarketTrends(allJobs);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get company insights
     */
    @GetMapping("/company/{companyName}")
    public ResponseEntity<Map<String, Object>> getCompanyInsights(@PathVariable String companyName) {
        try {
            List<JobListing> allJobs = jobListingRepository.findAll();
            Map<String, Object> insights = jobAnalyticsService.getCompanyInsights(companyName, allJobs);
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get salary insights for skills
     */
    @GetMapping("/salary/insights")
    public ResponseEntity<Map<String, Object>> getSalaryInsights(
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, defaultValue = "0") Integer experience) {
        try {
            if (salaryPredictionService == null) {
                return ResponseEntity.ok(new HashMap<>());
            }
            Map<String, Object> insights = salaryPredictionService.getSalaryInsights(
                skills, location, experience
            );
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}

