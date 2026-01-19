package com.resumeopt.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resumeopt.model.JobListing;

/**
 * Service for Perplexity AI API integration
 * Uses Perplexity's online models for real-time job market insights
 */
@Service
public class PerplexityService {
    
    @Value("${perplexity.api.key}")
    private String apiKey;
    
    @Value("${perplexity.api.url}")
    private String apiUrl;
    
    @Value("${perplexity.model:llama-3.1-sonar-small-128k-online}")
    private String model;
    
    @Value("${perplexity.enabled:true}")
    private boolean enabled;
    
    private final HttpClient httpClient;
    
    public PerplexityService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * Search for real-time job market trends using Perplexity
     */
    public String getJobMarketTrends(String role, String location) {
        if (!enabled) {
            System.out.println("Perplexity API is disabled");
            return "Perplexity API is disabled";
        }
        
        try {
            String prompt = String.format(
                "What are the current job market trends for %s positions in %s? " +
                "Provide a brief summary (3-4 sentences) focusing on demand, salary ranges, and required skills.",
                role, location
            );
            
            String response = callPerplexity(prompt);
            System.out.println("Perplexity provided job market trends for " + role);
            return response;
        } catch (Exception e) {
            System.err.println("Perplexity API error: " + e.getMessage());
            return "Unable to fetch job market trends at this time.";
        }
    }
    
    /**
     * Get real-time information about companies hiring
     */
    public String getCompanyHiringInfo(String companyName) {
        if (!enabled) {
            return "Perplexity API is disabled";
        }
        
        try {
            String prompt = String.format(
                "Is %s currently hiring for entry-level positions? " +
                "Provide brief information about their hiring status and what they look for in candidates.",
                companyName
            );
            
            return callPerplexity(prompt);
        } catch (Exception e) {
            System.err.println("Perplexity company info error: " + e.getMessage());
            return "Unable to fetch company information at this time.";
        }
    }
    
    /**
     * Get skill recommendations based on current market
     */
    public String getSkillRecommendations(String currentRole, String targetRole) {
        if (!enabled) {
            return "Perplexity API is disabled";
        }
        
        try {
            String prompt = String.format(
                "What skills should someone with %s experience learn to transition to %s? " +
                "List the top 5 most in-demand skills based on current job market data.",
                currentRole, targetRole
            );
            
            return callPerplexity(prompt);
        } catch (Exception e) {
            System.err.println("Perplexity skill recommendations error: " + e.getMessage());
            return "Unable to fetch skill recommendations at this time.";
        }
    }
    
    /**
     * Search for latest job openings using Perplexity's online search
     */
    public List<JobListing> searchLatestJobs(String searchQuery, String location) {
        if (!enabled) {
            System.out.println("Perplexity API is disabled");
            return new ArrayList<>();
        }
        
        List<JobListing> jobs = new ArrayList<>();
        
        try {
            String prompt = String.format(
                "Find the latest 10 entry-level %s job openings in %s posted in the last 3 days. " +
                "For each job, provide: Job Title, Company Name, Brief Description, and Apply URL if available. " +
                "Format as JSON array with fields: title, company, description, applyUrl",
                searchQuery, location
            );
            
            String response = callPerplexity(prompt);
            jobs = parseJobListingsFromResponse(response);
            
            System.out.println("Perplexity found " + jobs.size() + " job listings");
        } catch (Exception e) {
            System.err.println("Perplexity job search error: " + e.getMessage());
        }
        
        return jobs;
    }
    
    /**
     * Get salary insights for a role
     */
    public String getSalaryInsights(String role, String location, int experience) {
        if (!enabled) {
            return "Perplexity API is disabled";
        }
        
        try {
            String prompt = String.format(
                "What is the current salary range for %s with %d years of experience in %s? " +
                "Provide brief insights based on recent market data.",
                role, experience, location
            );
            
            return callPerplexity(prompt);
        } catch (Exception e) {
            System.err.println("Perplexity salary insights error: " + e.getMessage());
            return "Unable to fetch salary insights at this time.";
        }
    }
    
    /**
     * Get interview preparation tips
     */
    public String getInterviewTips(String role, String company) {
        if (!enabled) {
            return "Perplexity API is disabled";
        }
        
        try {
            String prompt = String.format(
                "What are the key interview preparation tips for a %s position at %s? " +
                "Provide 5 specific tips based on recent interview experiences.",
                role, company
            );
            
            return callPerplexity(prompt);
        } catch (Exception e) {
            System.err.println("Perplexity interview tips error: " + e.getMessage());
            return "Unable to fetch interview tips at this time.";
        }
    }
    
    /**
     * Call Perplexity API
     */
    private String callPerplexity(String prompt) throws IOException, InterruptedException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.2);
        requestBody.put("max_tokens", 1000);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            String errorMsg = "Perplexity API returned status: " + response.statusCode();
            // Provide more helpful error messages
            if (response.statusCode() == 401) {
                errorMsg += " - Invalid API key. Please check your perplexity.api.key in application.properties";
            } else if (response.statusCode() == 429) {
                errorMsg += " - Rate limit exceeded. Please try again later";
            } else if (response.statusCode() >= 500) {
                errorMsg += " - Perplexity service error. Please try again later";
            }
            throw new IOException(errorMsg);
        }
        
        JSONObject jsonResponse = new JSONObject(response.body());
        JSONArray choices = jsonResponse.getJSONArray("choices");
        if (choices.length() == 0) {
            throw new IOException("Perplexity API returned empty response");
        }
        return choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
    
    /**
     * Parse job listings from Perplexity response
     */
    private List<JobListing> parseJobListingsFromResponse(String response) {
        List<JobListing> jobs = new ArrayList<>();
        
        try {
            // Try to extract JSON from response
            String jsonStr = response;
            if (response.contains("```json")) {
                jsonStr = response.substring(response.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            } else if (response.contains("[")) {
                jsonStr = response.substring(response.indexOf("["));
                if (jsonStr.contains("]")) {
                    jsonStr = jsonStr.substring(0, jsonStr.lastIndexOf("]") + 1);
                }
            }
            
            JSONArray jobsArray = new JSONArray(jsonStr.trim());
            
            for (int i = 0; i < jobsArray.length(); i++) {
                JSONObject jobObj = jobsArray.getJSONObject(i);
                
                JobListing job = new JobListing();
                job.setTitle(jobObj.optString("title", "Software Engineer"));
                job.setCompany(jobObj.optString("company", "Tech Company"));
                job.setDescription(jobObj.optString("description", "Entry-level position from Perplexity AI"));
                job.setApplyUrl(jobObj.optString("applyUrl", "https://example.com/apply"));
                
                jobs.add(job);
            }
        } catch (Exception e) {
            System.err.println("Error parsing Perplexity response: " + e.getMessage());
        }
        
        return jobs;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
