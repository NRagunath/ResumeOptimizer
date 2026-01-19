package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Salary prediction service based on skills, experience, location, and company
 */
@Service
public class SalaryPredictionService {
    
    // Base salary ranges by experience level (in LPA)
    private static final Map<Integer, double[]> EXPERIENCE_SALARY_RANGES = createSalaryRanges();
    
    private static Map<Integer, double[]> createSalaryRanges() {
        Map<Integer, double[]> map = new HashMap<>();
        map.put(0, new double[]{2.5, 4.5});   // Fresher
        map.put(1, new double[]{3.5, 6.0});   // 1 year
        map.put(2, new double[]{5.0, 8.0});   // 2 years
        map.put(3, new double[]{7.0, 12.0});  // 3+ years
        return Collections.unmodifiableMap(map);
    }
    
    // Skill multipliers (premium skills increase salary)
    private static final Map<String, Double> SKILL_MULTIPLIERS = createSkillMultipliers();
    private static final Map<String, Double> LOCATION_MULTIPLIERS = createLocationMultipliers();
    private static final Map<String, Double> COMPANY_TIERS = createCompanyTiers();
    
    private static Map<String, Double> createSkillMultipliers() {
        Map<String, Double> map = new HashMap<>();
        map.put("machine learning", 1.3);
        map.put("artificial intelligence", 1.35);
        map.put("data science", 1.25);
        map.put("blockchain", 1.2);
        map.put("cloud", 1.15);
        map.put("aws", 1.2);
        map.put("azure", 1.15);
        map.put("kubernetes", 1.25);
        map.put("docker", 1.1);
        map.put("microservices", 1.15);
        map.put("react", 1.1);
        map.put("angular", 1.1);
        map.put("node.js", 1.1);
        map.put("python", 1.05);
        map.put("java", 1.05);
        return Collections.unmodifiableMap(map);
    }
    
    private static Map<String, Double> createLocationMultipliers() {
        Map<String, Double> map = new HashMap<>();
        map.put("bangalore", 1.2);
        map.put("mumbai", 1.15);
        map.put("hyderabad", 1.1);
        map.put("pune", 1.1);
        map.put("chennai", 1.05);
        map.put("delhi", 1.1);
        map.put("remote", 0.95);
        map.put("hybrid", 1.0);
        return Collections.unmodifiableMap(map);
    }
    
    private static Map<String, Double> createCompanyTiers() {
        Map<String, Double> map = new HashMap<>();
        map.put("faang", 1.5);
        map.put("tier1", 1.3);
        map.put("tier2", 1.1);
        map.put("startup", 0.9);
        map.put("default", 1.0);
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * Predict salary range for a job based on multiple factors
     */
    public String predictSalary(JobListing job, String userSkills) {
        if (job == null) {
            return "Not available";
        }
        
        // Get base salary from experience
        Integer experience = job.getExperienceRequired() != null ? 
            job.getExperienceRequired() : 0;
        experience = Math.min(experience, 3); // Cap at 3 years for freshers
        
        double[] baseRange = EXPERIENCE_SALARY_RANGES.getOrDefault(
            experience, 
            EXPERIENCE_SALARY_RANGES.get(0)
        );
        
        double minSalary = baseRange[0];
        double maxSalary = baseRange[1];
        
        // Apply skill multipliers
        if (userSkills != null && !userSkills.isBlank()) {
            double skillMultiplier = calculateSkillMultiplier(userSkills, job.getDescription());
            minSalary *= skillMultiplier;
            maxSalary *= skillMultiplier;
        }
        
        // Apply location multiplier
        if (job.getLocation() != null) {
            double locationMultiplier = calculateLocationMultiplier(job.getLocation());
            minSalary *= locationMultiplier;
            maxSalary *= locationMultiplier;
        }
        
        // Apply company tier multiplier
        double companyMultiplier = calculateCompanyMultiplier(job.getCompany());
        minSalary *= companyMultiplier;
        maxSalary *= companyMultiplier;
        
        // Round to 1 decimal place
        minSalary = Math.round(minSalary * 10.0) / 10.0;
        maxSalary = Math.round(maxSalary * 10.0) / 10.0;
        
        return String.format("%.1f - %.1f LPA", minSalary, maxSalary);
    }
    
    /**
     * Calculate skill multiplier based on premium skills
     */
    private double calculateSkillMultiplier(String userSkills, String jobDescription) {
        if (userSkills == null || userSkills.isBlank()) {
            return 1.0;
        }
        
        String combined = (userSkills + " " + (jobDescription != null ? jobDescription : "")).toLowerCase();
        double maxMultiplier = 1.0;
        
        for (Map.Entry<String, Double> entry : SKILL_MULTIPLIERS.entrySet()) {
            if (combined.contains(entry.getKey())) {
                maxMultiplier = Math.max(maxMultiplier, entry.getValue());
            }
        }
        
        // Average of base and max (to not over-inflate)
        return (1.0 + maxMultiplier) / 2.0;
    }
    
    /**
     * Calculate location multiplier
     */
    private double calculateLocationMultiplier(String location) {
        if (location == null) {
            return 1.0;
        }
        
        String locationLower = location.toLowerCase();
        for (Map.Entry<String, Double> entry : LOCATION_MULTIPLIERS.entrySet()) {
            if (locationLower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return 1.0;
    }
    
    /**
     * Calculate company tier multiplier
     */
    private double calculateCompanyMultiplier(String company) {
        if (company == null) {
            return COMPANY_TIERS.get("default");
        }
        
        String companyLower = company.toLowerCase();
        
        // FAANG companies
        String[] faang = {"google", "facebook", "amazon", "apple", "netflix", "microsoft"};
        for (String f : faang) {
            if (companyLower.contains(f)) {
                return COMPANY_TIERS.get("faang");
            }
        }
        
        // Tier 1 product companies
        String[] tier1 = {"adobe", "oracle", "salesforce", "sap", "vmware", "intel"};
        for (String t : tier1) {
            if (companyLower.contains(t)) {
                return COMPANY_TIERS.get("tier1");
            }
        }
        
        // Startups
        String[] startups = {"startup", "labs", "ventures", "innovations"};
        for (String s : startups) {
            if (companyLower.contains(s)) {
                return COMPANY_TIERS.get("startup");
            }
        }
        
        return COMPANY_TIERS.get("default");
    }
    
    /**
     * Get salary insights for a skill set
     */
    public Map<String, Object> getSalaryInsights(String skills, String location, Integer experience) {
        Map<String, Object> insights = new HashMap<>();
        
        experience = experience != null ? Math.min(experience, 3) : 0;
        double[] baseRange = EXPERIENCE_SALARY_RANGES.getOrDefault(
            experience, 
            EXPERIENCE_SALARY_RANGES.get(0)
        );
        
        // Calculate with skills
        double skillMultiplier = calculateSkillMultiplier(skills, "");
        double locationMultiplier = location != null ? 
            calculateLocationMultiplier(location) : 1.0;
        
        double minSalary = baseRange[0] * skillMultiplier * locationMultiplier;
        double maxSalary = baseRange[1] * skillMultiplier * locationMultiplier;
        
        insights.put("predictedRange", String.format("%.1f - %.1f LPA", minSalary, maxSalary));
        insights.put("baseRange", String.format("%.1f - %.1f LPA", baseRange[0], baseRange[1]));
        insights.put("skillMultiplier", skillMultiplier);
        insights.put("locationMultiplier", locationMultiplier);
        
        // Top paying skills
        List<String> topSkills = new ArrayList<>();
        if (skills != null) {
            String skillsLower = skills.toLowerCase();
            for (Map.Entry<String, Double> entry : SKILL_MULTIPLIERS.entrySet()) {
                if (skillsLower.contains(entry.getKey()) && entry.getValue() > 1.2) {
                    topSkills.add(entry.getKey());
                }
            }
        }
        insights.put("premiumSkills", topSkills);
        
        return insights;
    }
}

