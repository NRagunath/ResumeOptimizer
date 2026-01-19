package com.resumeopt.scheduler;

import com.resumeopt.model.Company;
import com.resumeopt.service.EnhancedCompanyDiscoveryService;
import com.resumeopt.repo.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompanyDiscoveryScheduler {
    
    @Autowired
    private EnhancedCompanyDiscoveryService enhancedCompanyDiscoveryService;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    /**
     * Runs daily at 2 AM to discover new companies and update existing ones
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void discoverAndUpdateCompanies() {
        System.out.println("CompanyDiscoveryScheduler: Starting daily company discovery and update...");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Check if we need to discover companies (if database is empty or has less than 50 companies)
            long existingCompanies = companyRepository.count();
            System.out.println("CompanyDiscoveryScheduler: Found " + existingCompanies + " existing companies");
            
            if (existingCompanies < 50) {
                System.out.println("CompanyDiscoveryScheduler: Discovering all companies from comprehensive list...");
                List<Company> discovered = enhancedCompanyDiscoveryService.discoverAllCompanies();
                System.out.println("CompanyDiscoveryScheduler: Discovered " + discovered.size() + " companies");
            } else {
                System.out.println("CompanyDiscoveryScheduler: Sufficient companies exist, validating URLs...");
                enhancedCompanyDiscoveryService.validateAndUpdateCompanyUrls();
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("CompanyDiscoveryScheduler: Company discovery completed in " + elapsed + "ms");
            
        } catch (Exception e) {
            System.err.println("CompanyDiscoveryScheduler: Error during company discovery: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Runs weekly on Sunday at 3 AM to validate and update all company URLs
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void validateCompanyUrls() {
        System.out.println("CompanyDiscoveryScheduler: Starting weekly URL validation...");
        
        try {
            long startTime = System.currentTimeMillis();
            
            enhancedCompanyDiscoveryService.validateAndUpdateCompanyUrls();
            
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("CompanyDiscoveryScheduler: URL validation completed in " + elapsed + "ms");
            
        } catch (Exception e) {
            System.err.println("CompanyDiscoveryScheduler: Error during URL validation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Manual trigger for company discovery (can be called via API)
     */
    public void triggerCompanyDiscovery() {
        System.out.println("CompanyDiscoveryScheduler: Manual company discovery triggered...");
        discoverAndUpdateCompanies();
    }
    
    /**
     * Manual trigger for URL validation (can be called via API)
     */
    public void triggerUrlValidation() {
        System.out.println("CompanyDiscoveryScheduler: Manual URL validation triggered...");
        validateCompanyUrls();
    }
}