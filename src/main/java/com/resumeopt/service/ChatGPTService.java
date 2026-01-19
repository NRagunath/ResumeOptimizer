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
 * Service for ChatGPT API integration
 * Uses OpenAI's GPT models for job search enhancement and recommendations
 */
@Service
public class ChatGPTService {
    
    @Value("${chatgpt.api.key}")
    private String apiKey;
    
    @Value("${chatgpt.api.url}")
    private String apiUrl;
    
    @Value("${chatgpt.model:gpt-4o-mini}")
    private String model;
    
    @Value("${chatgpt.enabled:true}")
    private boolean enabled;
    
    private final HttpClient httpClient;
    
    public ChatGPTService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * Generate job listings using ChatGPT based on search criteria
     */
    public List<JobListing> generateJobListings(String searchQuery) {
        if (!enabled) {
            System.out.println("ChatGPT API is disabled");
            return new ArrayList<>();
        }
        
        List<JobListing> jobs = new ArrayList<>();
        
        try {
            String prompt = buildJobSearchPrompt(searchQuery);
            String response = callChatGPT(prompt);
            jobs = parseJobListingsFromResponse(response);
            
            System.out.println("ChatGPT generated " + jobs.size() + " job listings");
        } catch (Exception e) {
            System.err.println("ChatGPT API error: " + e.getMessage());
        }
        
        return jobs;
    }
    
    /**
     * Enhance job description using ChatGPT
     */
    public String enhanceJobDescription(String originalDescription) {
        if (!enabled) {
            return originalDescription;
        }
        
        try {
            String prompt = "Enhance this job description to be more appealing for entry-level candidates. Keep it concise (2-3 sentences):\n\n" + originalDescription;
            return callChatGPT(prompt);
        } catch (Exception e) {
            System.err.println("ChatGPT enhancement error: " + e.getMessage());
            return originalDescription;
        }
    }
    
    /**
     * Get career advice
     */
    public String getCareerAdvice(String userSkills, String targetRole) {
        if (!enabled) {
            return "ChatGPT API is disabled";
        }
        
        try {
            String prompt = String.format(
                "As a career advisor, provide brief advice (3-4 sentences) for someone with skills in %s who wants to become a %s. Focus on practical next steps.",
                userSkills, targetRole
            );
            return callChatGPT(prompt);
        } catch (Exception e) {
            System.err.println("ChatGPT advice error: " + e.getMessage());
            return "Unable to generate career advice at this time.";
        }
    }
    
    /**
     * Analyze resume and suggest improvements
     */
    public String analyzeResume(String resumeText) {
        if (!enabled) {
            return "ChatGPT API is disabled";
        }
        
        try {
            String prompt = "Analyze this resume for an entry-level IT position and provide 3 key improvement suggestions:\n\n" + resumeText;
            return callChatGPT(prompt);
        } catch (Exception e) {
            System.err.println("ChatGPT resume analysis error: " + e.getMessage());
            return "Unable to analyze resume at this time.";
        }
    }
    
    /**
     * Call ChatGPT API
     */
    private String callChatGPT(String prompt) throws IOException, InterruptedException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
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
            String errorMsg = "ChatGPT API returned status: " + response.statusCode();
            // Provide more helpful error messages
            if (response.statusCode() == 401) {
                errorMsg += " - Invalid API key. Please check your chatgpt.api.key in application.properties";
            } else if (response.statusCode() == 429) {
                errorMsg += " - Rate limit exceeded. Please try again later";
            } else if (response.statusCode() >= 500) {
                errorMsg += " - OpenAI service error. Please try again later";
            }
            throw new IOException(errorMsg);
        }
        
        JSONObject jsonResponse = new JSONObject(response.body());
        JSONArray choices = jsonResponse.getJSONArray("choices");
        if (choices.length() == 0) {
            throw new IOException("ChatGPT API returned empty response");
        }
        return choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
    
    /**
     * Build prompt for job search
     */
    private String buildJobSearchPrompt(String searchQuery) {
        return String.format(
            "Generate 10 realistic entry-level IT job listings in India for: %s. " +
            "For each job, provide: Job Title, Company Name, Brief Description (1-2 sentences), and a realistic apply URL. " +
            "Format as JSON array with fields: title, company, description, applyUrl",
            searchQuery
        );
    }
    
    /**
     * Parse job listings from ChatGPT response
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
                job.setDescription(jobObj.optString("description", "Entry-level position"));
                job.setApplyUrl(jobObj.optString("applyUrl", "https://example.com/apply"));
                
                jobs.add(job);
            }
        } catch (Exception e) {
            System.err.println("Error parsing ChatGPT response: " + e.getMessage());
        }
        
        return jobs;
    }
    
    /**
     * Get fresher-specific career advice
     */
    public String getFresherCareerAdvice(String resumeText, String jobDescription, String academicBackground) {
        if (!enabled) {
            return "ChatGPT API is disabled";
        }
        
        try {
            String prompt = String.format(
                "As a career advisor for freshers (entry-level candidates), provide comprehensive advice for someone with this background: %s. " +
                "They have this resume: %s. " +
                "They are interested in this role: %s. " +
                "Provide specific advice on: 1) How to tailor their application, 2) What skills to develop, 3) Interview preparation tips, " +
                "4) Salary expectations for freshers, and 5) Long-term career growth strategy. Keep responses concise but actionable.",
                academicBackground, resumeText, jobDescription
            );
            return callChatGPT(prompt);
        } catch (Exception e) {
            System.err.println("ChatGPT fresher advice error: " + e.getMessage());
            return "Unable to generate fresher career advice at this time.";
        }
    }
    
    /**
     * Analyze fresher resume for job matching
     */
    public String analyzeFresherResumeForJob(String resumeText, String jobDescription) {
        if (!enabled) {
            return "ChatGPT API is disabled";
        }
        
        try {
            String prompt = String.format(
                "Analyze how well this fresher's resume matches the job description. " +
                "Resume: %s. Job Description: %s. " +
                "Provide: 1) Match percentage (0-100), 2) Key strengths that align, 3) Major gaps to address, " +
                "4) Specific suggestions to improve the resume for this role, 5) Keywords to include. Keep it concise.",
                resumeText, jobDescription
            );
            return callChatGPT(prompt);
        } catch (Exception e) {
            System.err.println("ChatGPT fresher resume analysis error: " + e.getMessage());
            return "Unable to analyze fresher resume at this time.";
        }
    }
    
    /**
     * Generate fresher-friendly job description enhancements
     */
    public String enhanceJobDescriptionForFreshers(String originalDescription) {
        if (!enabled) {
            return originalDescription;
        }
        
        try {
            String prompt = "Enhance this job description to be more welcoming and clear for entry-level/fresher candidates. " +
                           "Highlight training opportunities, mentorship programs, learning support, and growth potential. " +
                           "Keep the requirements but frame them positively for someone with limited experience. " +
                           "Original description: " + originalDescription;
            return callChatGPT(prompt);
        } catch (Exception e) {
            System.err.println("ChatGPT job description enhancement error: " + e.getMessage());
            return originalDescription;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
