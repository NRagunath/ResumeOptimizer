package com.resumeopt.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Company reputation and rating service
 */
@Service
public class CompanyReputationService {
    
    // Company reputation scores (0.0 to 1.0)
    private static final Map<String, Double> COMPANY_REPUTATIONS = createReputationMap();
    
    private static Map<String, Double> createReputationMap() {
        Map<String, Double> map = new HashMap<>();
        
        // Tier 1 - FAANG and top companies (0.9-1.0)
        map.put("google", 1.0);
        map.put("microsoft", 0.98);
        map.put("amazon", 0.95);
        map.put("apple", 0.97);
        map.put("netflix", 0.94);
        map.put("facebook", 0.96);
        map.put("meta", 0.96);
        
        // Tier 1 - Top product companies (0.85-0.95)
        map.put("adobe", 0.92);
        map.put("oracle", 0.90);
        map.put("salesforce", 0.91);
        map.put("sap", 0.88);
        map.put("vmware", 0.87);
        map.put("intel", 0.89);
        
        // Tier 2 - Major IT services (0.75-0.85)
        map.put("tcs", 0.82);
        map.put("infosys", 0.80);
        map.put("wipro", 0.78);
        map.put("hcl", 0.79);
        map.put("cognizant", 0.81);
        map.put("accenture", 0.83);
        map.put("capgemini", 0.77);
        map.put("tech mahindra", 0.76);
        
        // Tier 3 - Indian startups and unicorns (0.70-0.80)
        map.put("flipkart", 0.78);
        map.put("razorpay", 0.75);
        map.put("phonepe", 0.76);
        map.put("zomato", 0.74);
        map.put("swiggy", 0.73);
        map.put("paytm", 0.72);
        map.put("byju", 0.71);
        map.put("freshworks", 0.77);
        map.put("zoho", 0.79);
        
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * Get reputation score for a company
     */
    public double getReputationScore(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return 0.5; // Default neutral score
        }
        
        String companyLower = companyName.toLowerCase();
        
        // Direct match
        if (COMPANY_REPUTATIONS.containsKey(companyLower)) {
            return COMPANY_REPUTATIONS.get(companyLower);
        }
        
        // Partial match
        for (Map.Entry<String, Double> entry : COMPANY_REPUTATIONS.entrySet()) {
            if (companyLower.contains(entry.getKey()) || entry.getKey().contains(companyLower)) {
                return entry.getValue();
            }
        }
        
        // Default score based on company size indicators
        if (companyLower.contains("startup") || companyLower.contains("labs") || 
            companyLower.contains("ventures")) {
            return 0.65; // Startup default
        }
        
        return 0.6; // Unknown company default
    }
    
    /**
     * Get company tier
     */
    public String getCompanyTier(String companyName) {
        double score = getReputationScore(companyName);
        
        if (score >= 0.9) return "Tier 1 - FAANG";
        if (score >= 0.85) return "Tier 1 - Top Product";
        if (score >= 0.75) return "Tier 2 - Major IT";
        if (score >= 0.70) return "Tier 3 - Unicorns/Startups";
        return "Tier 4 - Other";
    }
    
    /**
     * Get company insights
     */
    public Map<String, Object> getCompanyInsights(String companyName) {
        Map<String, Object> insights = new HashMap<>();
        
        double reputation = getReputationScore(companyName);
        String tier = getCompanyTier(companyName);
        
        insights.put("reputationScore", reputation);
        insights.put("tier", tier);
        insights.put("rating", Math.round(reputation * 5 * 10) / 10.0); // 5-star rating
        
        // Benefits typically offered
        if (reputation >= 0.85) {
            insights.put("typicalBenefits", Arrays.asList(
                "Excellent compensation", "Health insurance", "Stock options",
                "Flexible work", "Learning budget", "Gym membership"
            ));
        } else if (reputation >= 0.75) {
            insights.put("typicalBenefits", Arrays.asList(
                "Good compensation", "Health insurance", "Learning opportunities",
                "Work-life balance"
            ));
        } else {
            insights.put("typicalBenefits", Arrays.asList(
                "Standard benefits", "Health insurance"
            ));
        }
        
        return insights;
    }
}

