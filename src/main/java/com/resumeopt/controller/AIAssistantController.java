package com.resumeopt.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.resumeopt.model.JobListing;
import com.resumeopt.service.ChatGPTService;
import com.resumeopt.service.PerplexityService;

/**
 * REST Controller for AI Assistant features
 * Provides endpoints for ChatGPT and Perplexity AI integrations
 */
@RestController
@RequestMapping("/api/ai")
public class AIAssistantController {
    
    @Autowired
    private ChatGPTService chatGPTService;
    
    @Autowired
    private PerplexityService perplexityService;
    
    /**
     * Get career advice from ChatGPT
     */
    @GetMapping("/career-advice")
    public ResponseEntity<Map<String, String>> getCareerAdvice(
            @RequestParam String skills,
            @RequestParam String targetRole) {
        
        String advice = chatGPTService.getCareerAdvice(skills, targetRole);
        
        Map<String, String> response = new HashMap<>();
        response.put("advice", advice);
        response.put("source", "ChatGPT");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Analyze resume using ChatGPT
     */
    @PostMapping("/analyze-resume")
    public ResponseEntity<Map<String, String>> analyzeResume(@RequestBody Map<String, String> request) {
        String resumeText = request.get("resumeText");
        String analysis = chatGPTService.analyzeResume(resumeText);
        
        Map<String, String> response = new HashMap<>();
        response.put("analysis", analysis);
        response.put("source", "ChatGPT");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Enhance job description using ChatGPT
     */
    @PostMapping("/enhance-description")
    public ResponseEntity<Map<String, String>> enhanceDescription(@RequestBody Map<String, String> request) {
        String originalDescription = request.get("description");
        String enhanced = chatGPTService.enhanceJobDescription(originalDescription);
        
        Map<String, String> response = new HashMap<>();
        response.put("original", originalDescription);
        response.put("enhanced", enhanced);
        response.put("source", "ChatGPT");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get job market trends from Perplexity
     */
    @GetMapping("/market-trends")
    public ResponseEntity<Map<String, String>> getMarketTrends(
            @RequestParam String role,
            @RequestParam(defaultValue = "India") String location) {
        
        String trends = perplexityService.getJobMarketTrends(role, location);
        
        Map<String, String> response = new HashMap<>();
        response.put("trends", trends);
        response.put("role", role);
        response.put("location", location);
        response.put("source", "Perplexity AI");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get company hiring information from Perplexity
     */
    @GetMapping("/company-info")
    public ResponseEntity<Map<String, String>> getCompanyInfo(@RequestParam String company) {
        String info = perplexityService.getCompanyHiringInfo(company);
        
        Map<String, String> response = new HashMap<>();
        response.put("info", info);
        response.put("company", company);
        response.put("source", "Perplexity AI");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get skill recommendations from Perplexity
     */
    @GetMapping("/skill-recommendations")
    public ResponseEntity<Map<String, String>> getSkillRecommendations(
            @RequestParam String currentRole,
            @RequestParam String targetRole) {
        
        String recommendations = perplexityService.getSkillRecommendations(currentRole, targetRole);
        
        Map<String, String> response = new HashMap<>();
        response.put("recommendations", recommendations);
        response.put("currentRole", currentRole);
        response.put("targetRole", targetRole);
        response.put("source", "Perplexity AI");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get salary insights from Perplexity
     */
    @GetMapping("/salary-insights")
    public ResponseEntity<Map<String, Object>> getSalaryInsights(
            @RequestParam String role,
            @RequestParam(defaultValue = "India") String location,
            @RequestParam(defaultValue = "0") int experience) {
        
        String insights = perplexityService.getSalaryInsights(role, location, experience);
        
        Map<String, Object> response = new HashMap<>();
        response.put("insights", insights);
        response.put("role", role);
        response.put("location", location);
        response.put("experience", experience);
        response.put("source", "Perplexity AI");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get interview tips from Perplexity
     */
    @GetMapping("/interview-tips")
    public ResponseEntity<Map<String, String>> getInterviewTips(
            @RequestParam String role,
            @RequestParam String company) {
        
        String tips = perplexityService.getInterviewTips(role, company);
        
        Map<String, String> response = new HashMap<>();
        response.put("tips", tips);
        response.put("role", role);
        response.put("company", company);
        response.put("source", "Perplexity AI");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generate job listings using ChatGPT
     */
    @GetMapping("/generate-jobs")
    public ResponseEntity<Map<String, Object>> generateJobs(@RequestParam String query) {
        List<JobListing> jobs = chatGPTService.generateJobListings(query);
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobs", jobs);
        response.put("count", jobs.size());
        response.put("query", query);
        response.put("source", "ChatGPT");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Search latest jobs using Perplexity
     */
    @GetMapping("/search-jobs")
    public ResponseEntity<Map<String, Object>> searchJobs(
            @RequestParam String query,
            @RequestParam(defaultValue = "India") String location) {
        
        List<JobListing> jobs = perplexityService.searchLatestJobs(query, location);
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobs", jobs);
        response.put("count", jobs.size());
        response.put("query", query);
        response.put("location", location);
        response.put("source", "Perplexity AI");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check AI services status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("chatgpt", chatGPTService.isEnabled() ? "enabled" : "disabled");
        response.put("perplexity", perplexityService.isEnabled() ? "enabled" : "disabled");
        
        return ResponseEntity.ok(response);
    }
}
