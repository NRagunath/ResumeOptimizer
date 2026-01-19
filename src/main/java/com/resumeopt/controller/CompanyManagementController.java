package com.resumeopt.controller;

import com.resumeopt.model.Company;
import com.resumeopt.model.CareerPagePlatform;
import com.resumeopt.service.EnhancedCompanyDiscoveryService;
import com.resumeopt.repo.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/companies")
public class CompanyManagementController {
    
    @Autowired
    private EnhancedCompanyDiscoveryService enhancedCompanyDiscoveryService;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    /**
     * Discover all 200+ companies from the comprehensive list
     */
    @PostMapping("/discover-all")
    public ResponseEntity<Map<String, Object>> discoverAllCompanies() {
        try {
            List<Company> discovered = enhancedCompanyDiscoveryService.discoverAllCompanies();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully discovered " + discovered.size() + " companies");
            response.put("companiesDiscovered", discovered.size());
            response.put("companies", discovered);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error discovering companies: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Discover IT/Software companies
     */
    @PostMapping("/discover-it-software-companies")
    public ResponseEntity<Map<String, Object>> discoverITSoftwareCompanies() {
        try {
            List<Company> discovered = enhancedCompanyDiscoveryService.discoverITAndSoftwareCompanies();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully discovered " + discovered.size() + " IT/Software companies");
            response.put("companiesDiscovered", discovered.size());
            response.put("companies", discovered);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error discovering IT/Software companies: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get high-volume hiring companies
     */
    @GetMapping("/high-volume-hiring")
    public ResponseEntity<Map<String, Object>> getHighVolumeHiringCompanies() {
        try {
            List<Company> companies = enhancedCompanyDiscoveryService.getHighVolumeHiringCompanies();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCompanies", companies.size());
            response.put("companies", companies);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching high-volume hiring companies: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get all companies with filtering options
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getCompanies(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Boolean isStartup,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isVerified) {
        
        try {
            List<Company> companies;
            
            if (industry != null && !industry.isBlank()) {
                companies = enhancedCompanyDiscoveryService.getCompaniesByIndustry(industry);
            } else if (platform != null && !platform.isBlank()) {
                CareerPagePlatform platformEnum = CareerPagePlatform.valueOf(platform.toUpperCase());
                companies = enhancedCompanyDiscoveryService.getCompaniesByPlatform(platformEnum);
            } else if (Boolean.TRUE.equals(isStartup)) {
                companies = enhancedCompanyDiscoveryService.getStartupCompanies();
            } else if (Boolean.TRUE.equals(isVerified)) {
                companies = enhancedCompanyDiscoveryService.getVerifiedCompanies();
            } else if (Boolean.TRUE.equals(isActive)) {
                companies = enhancedCompanyDiscoveryService.getActiveCompanies();
            } else {
                companies = companyRepository.findAll();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCompanies", companies.size());
            response.put("companies", companies);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching companies: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get company statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCompanyStats() {
        try {
            long totalCompanies = companyRepository.count();
            long activeCompanies = companyRepository.findByIsActiveTrue().size();
            long startupCompanies = companyRepository.findByIsStartupTrue().size();
            long verifiedCompanies = companyRepository.findByIsVerifiedTrue().size();
            
            // Count by platform
            Map<String, Long> platformCounts = new HashMap<>();
            for (CareerPagePlatform platform : CareerPagePlatform.values()) {
                long count = companyRepository.findByPlatform(platform).size();
                platformCounts.put(platform.name(), count);
            }
            
            // Count by industry (top 10)
            Map<String, Long> industryCounts = new HashMap<>();
            List<Company> allCompanies = companyRepository.findAll();
            allCompanies.stream()
                .filter(c -> c.getIndustry() != null && !c.getIndustry().isBlank())
                .forEach(c -> {
                    String industry = c.getIndustry();
                    industryCounts.put(industry, industryCounts.getOrDefault(industry, 0L) + 1);
                });
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCompanies", totalCompanies);
            response.put("activeCompanies", activeCompanies);
            response.put("startupCompanies", startupCompanies);
            response.put("verifiedCompanies", verifiedCompanies);
            response.put("platformDistribution", platformCounts);
            response.put("industryDistribution", industryCounts);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching company stats: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Update company status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateCompanyStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive) {
        
        try {
            enhancedCompanyDiscoveryService.updateCompanyScrapingStatus(id, isActive, 0);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Company status updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating company status: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Validate and update all company URLs
     */
    @PostMapping("/validate-urls")
    public ResponseEntity<Map<String, Object>> validateCompanyUrls() {
        try {
            enhancedCompanyDiscoveryService.validateAndUpdateCompanyUrls();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Company URL validation completed");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error validating company URLs: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get companies by platform
     */
    @GetMapping("/platform/{platform}")
    public ResponseEntity<Map<String, Object>> getCompaniesByPlatform(@PathVariable String platform) {
        try {
            CareerPagePlatform platformEnum = CareerPagePlatform.valueOf(platform.toUpperCase());
            List<Company> companies = enhancedCompanyDiscoveryService.getCompaniesByPlatform(platformEnum);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("platform", platform);
            response.put("totalCompanies", companies.size());
            response.put("companies", companies);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid platform: " + platform);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching companies by platform: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Register a new company manually
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerCompany(
            @RequestParam String name,
            @RequestParam String careerPageUrl,
            @RequestParam(required = false) String industry) {
        
        try {
            Company company = enhancedCompanyDiscoveryService.registerCompany(name, careerPageUrl);
            if (company != null) {
                if (industry != null && !industry.isBlank()) {
                    company.setIndustry(industry);
                }
                companyRepository.save(company);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Company registered successfully");
                response.put("company", company);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to register company");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error registering company: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}