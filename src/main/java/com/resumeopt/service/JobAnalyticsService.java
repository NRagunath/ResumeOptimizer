package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced analytics service for job market insights
 */
@Service
public class JobAnalyticsService {
    
    /**
     * Get market trends analysis
     */
    public Map<String, Object> analyzeMarketTrends(List<JobListing> jobs) {
        Map<String, Object> trends = new HashMap<>();
        
        if (jobs == null || jobs.isEmpty()) {
            return trends;
        }
        
        // Job type distribution
        Map<String, Long> jobTypeDistribution = jobs.stream()
                .filter(j -> j.getJobType() != null)
                .collect(Collectors.groupingBy(
                    j -> j.getJobType().name(),
                    Collectors.counting()
                ));
        trends.put("jobTypeDistribution", jobTypeDistribution);
        
        // Top companies
        Map<String, Long> topCompanies = jobs.stream()
                .filter(j -> j.getCompany() != null)
                .collect(Collectors.groupingBy(
                    JobListing::getCompany,
                    Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        trends.put("topCompanies", topCompanies);
        
        // Location distribution
        Map<String, Long> locationDistribution = jobs.stream()
                .filter(j -> j.getLocation() != null)
                .collect(Collectors.groupingBy(
                    JobListing::getLocation,
                    Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        trends.put("topLocations", locationDistribution);
        
        // Skill frequency analysis
        Map<String, Long> skillFrequency = analyzeSkillFrequency(jobs);
        trends.put("topSkills", skillFrequency);
        
        // Average salary range
        String avgSalary = calculateAverageSalary(jobs);
        trends.put("averageSalary", avgSalary);
        
        // Job posting trend (last 7 days)
        long jobsLastWeek = jobs.stream()
                .filter(j -> j.getPostedDate() != null)
                .filter(j -> j.getPostedDate().isAfter(LocalDateTime.now().minusDays(7)))
                .count();
        trends.put("jobsLastWeek", jobsLastWeek);
        
        // Source distribution
        Map<String, Long> sourceDistribution = jobs.stream()
                .filter(j -> j.getSource() != null)
                .collect(Collectors.groupingBy(
                    j -> j.getSource().name(),
                    Collectors.counting()
                ));
        trends.put("sourceDistribution", sourceDistribution);
        
        return trends;
    }
    
    /**
     * Analyze skill frequency in job descriptions
     */
    private Map<String, Long> analyzeSkillFrequency(List<JobListing> jobs) {
        Map<String, Long> skillCount = new HashMap<>();
        
        String[] commonSkills = {
            "java", "python", "javascript", "react", "angular", "node.js", "spring",
            "sql", "mongodb", "aws", "docker", "kubernetes", "git", "agile",
            "machine learning", "data science", "devops", "microservices"
        };
        
        for (JobListing job : jobs) {
            if (job.getDescription() == null) continue;
            
            String desc = job.getDescription().toLowerCase();
            for (String skill : commonSkills) {
                if (desc.contains(skill)) {
                    skillCount.put(skill, skillCount.getOrDefault(skill, 0L) + 1);
                }
            }
        }
        
        return skillCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
    }
    
    /**
     * Calculate average salary from job listings
     */
    private String calculateAverageSalary(List<JobListing> jobs) {
        List<Double> salaries = new ArrayList<>();
        
        for (JobListing job : jobs) {
            if (job.getSalaryRange() == null) continue;
            
            String salary = job.getSalaryRange();
            // Extract numbers from salary range (e.g., "3-5 LPA" -> [3, 5])
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher matcher = pattern.matcher(salary);
            
            List<Double> range = new ArrayList<>();
            while (matcher.find()) {
                range.add(Double.parseDouble(matcher.group(1)));
            }
            
            if (!range.isEmpty()) {
                double avg = range.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                salaries.add(avg);
            }
        }
        
        if (salaries.isEmpty()) {
            return "Not available";
        }
        
        double average = salaries.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return String.format("%.1f LPA", average);
    }
    
    /**
     * Get company insights
     */
    public Map<String, Object> getCompanyInsights(String companyName, List<JobListing> allJobs) {
        Map<String, Object> insights = new HashMap<>();
        
        List<JobListing> companyJobs = allJobs.stream()
                .filter(j -> j.getCompany() != null && j.getCompany().equalsIgnoreCase(companyName))
                .collect(Collectors.toList());
        
        if (companyJobs.isEmpty()) {
            return insights;
        }
        
        insights.put("totalJobs", companyJobs.size());
        insights.put("activeListings", companyJobs.size());
        
        // Job types
        Map<String, Long> jobTypes = companyJobs.stream()
                .filter(j -> j.getJobType() != null)
                .collect(Collectors.groupingBy(
                    j -> j.getJobType().name(),
                    Collectors.counting()
                ));
        insights.put("jobTypes", jobTypes);
        
        // Locations
        Set<String> locations = companyJobs.stream()
                .filter(j -> j.getLocation() != null)
                .map(JobListing::getLocation)
                .collect(Collectors.toSet());
        insights.put("locations", locations);
        
        // Average salary
        String avgSalary = calculateAverageSalary(companyJobs);
        insights.put("averageSalary", avgSalary);
        
        return insights;
    }
    
    /**
     * Enrich job listing from description
     */
    public void enrichFromDescription(JobListing job) {
        if (job == null || job.getDescription() == null) {
            return;
        }
        
        // Extract required skills from description
        String[] commonSkills = {
            "java", "python", "javascript", "react", "angular", "node.js", "spring",
            "sql", "mongodb", "aws", "docker", "kubernetes", "git", "agile"
        };
        
        List<String> requiredSkills = new ArrayList<>();
        String desc = job.getDescription().toLowerCase();
        for (String skill : commonSkills) {
            if (desc.contains(skill)) {
                requiredSkills.add(skill);
            }
        }
        
        job.setRequiredSkills(requiredSkills);
    }
    
    /**
     * Estimate success probability based on skills match
     */
    public double estimateSuccessProbability(String userSkills, JobListing job) {
        if (userSkills == null || userSkills.isBlank() || job == null) {
            return 0.5;
        }
        
        List<String> requiredSkills = job.getRequiredSkills();
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            enrichFromDescription(job);
            requiredSkills = job.getRequiredSkills();
        }
        
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 0.5;
        }
        
        String[] userSkillArray = userSkills.toLowerCase().split("[,\\s]+");
        int matches = 0;
        for (String skill : userSkillArray) {
            if (requiredSkills.stream().anyMatch(rs -> rs.contains(skill.trim()) || skill.trim().contains(rs))) {
                matches++;
            }
        }
        
        return (double) matches / Math.max(requiredSkills.size(), userSkillArray.length);
    }
    
    /**
     * Find skill gaps between user skills and job requirements
     */
    public List<String> findSkillGaps(String userSkills, JobListing job) {
        List<String> gaps = new ArrayList<>();
        
        if (userSkills == null || userSkills.isBlank() || job == null) {
            return gaps;
        }
        
        List<String> requiredSkills = job.getRequiredSkills();
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            enrichFromDescription(job);
            requiredSkills = job.getRequiredSkills();
        }
        
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return gaps;
        }
        
        String userSkillsLower = userSkills.toLowerCase();
        for (String skill : requiredSkills) {
            if (!userSkillsLower.contains(skill.toLowerCase())) {
                gaps.add(skill);
            }
        }
        
        return gaps;
    }
    
    /**
     * Get guidance tips based on skill gaps
     */
    public List<String> guidanceTips(List<String> skillGaps) {
        List<String> tips = new ArrayList<>();
        
        if (skillGaps == null || skillGaps.isEmpty()) {
            return tips;
        }
        
        for (String gap : skillGaps) {
            tips.add("Consider learning " + gap + " to improve your chances");
        }
        
        return tips;
    }
}
