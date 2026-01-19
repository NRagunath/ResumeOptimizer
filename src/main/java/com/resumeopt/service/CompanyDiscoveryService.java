package com.resumeopt.service;

import com.resumeopt.model.Company;
import com.resumeopt.model.CareerPagePlatform;
import com.resumeopt.repo.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompanyDiscoveryService {
    
    @Autowired
    private CompanyRepository companyRepository;
    
    // Seed list of major Indian IT companies and startups
    private static final String[][] SEED_COMPANIES = {
        // Major IT Companies
        {"TCS", "https://www.tcs.com/careers", "IT Services"},
        {"Infosys", "https://www.infosys.com/careers", "IT Services"},
        {"Wipro", "https://careers.wipro.com", "IT Services"},
        {"HCL", "https://www.hcltech.com/careers", "IT Services"},
        {"Tech Mahindra", "https://careers.techmahindra.com", "IT Services"},
        {"Cognizant", "https://careers.cognizant.com", "IT Services"},
        {"Accenture", "https://www.accenture.com/in-en/careers", "IT Services"},
        {"Capgemini", "https://www.capgemini.com/in-en/careers", "IT Services"},
        {"L&T Infotech", "https://www.lntinfotech.com/careers", "IT Services"},
        {"Mindtree", "https://www.mindtree.com/careers", "IT Services"},
        
        // Product Companies
        {"Flipkart", "https://www.flipkartcareers.com", "E-commerce"},
        {"Amazon India", "https://www.amazon.jobs/en/locations/india", "E-commerce"},
        {"Microsoft India", "https://careers.microsoft.com/us/en/search-results?keywords=india", "Software"},
        {"Google India", "https://careers.google.com/locations/india/", "Software"},
        {"Adobe India", "https://careers.adobe.com/us/en/search-results?keywords=india", "Software"},
        {"Oracle India", "https://careers.oracle.com/jobs/#en/sites/jobsearch/job/", "Software"},
        {"SAP India", "https://jobs.sap.com/search/?q=india", "Software"},
        {"Salesforce India", "https://salesforce.wd1.myworkdayjobs.com/en-US/External_Careers", "Software"},
        
        // Startups
        {"Zomato", "https://www.zomato.com/careers", "Food Tech"},
        {"Swiggy", "https://careers.swiggy.com", "Food Tech"},
        {"Ola", "https://www.olacabs.com/careers", "Mobility"},
        {"Razorpay", "https://razorpay.com/jobs/", "Fintech"},
        {"PhonePe", "https://www.phonepe.com/careers", "Fintech"},
        {"Paytm", "https://paytm.com/careers", "Fintech"},
        {"Byju's", "https://byjus.com/careers/", "EdTech"},
        {"Unacademy", "https://unacademy.com/careers", "EdTech"},
        {"Cred", "https://cred.club/careers", "Fintech"},
        {"Meesho", "https://careers.meesho.com", "E-commerce"},
        {"Nykaa", "https://www.nykaa.com/careers", "E-commerce"},
        {"PolicyBazaar", "https://www.policybazaar.com/careers", "Fintech"},
        {"Groww", "https://groww.in/careers", "Fintech"},
        {"Dream11", "https://www.dream11.com/careers", "Gaming"},
        {"ShareChat", "https://sharechat.com/careers", "Social Media"},
        {"DailyHunt", "https://dailyhunt.in/careers", "Media"},
        {"InMobi", "https://www.inmobi.com/company/careers", "AdTech"},
        {"Freshworks", "https://www.freshworks.com/company/careers/", "SaaS"},
        {"Zoho", "https://www.zoho.com/careers", "SaaS"},
        {"BrowserStack", "https://www.browserstack.com/careers", "SaaS"}
    };
    
    /**
     * Discovers companies from seed list and registers them
     */
    public List<Company> discoverCompaniesFromSeedList() {
        List<Company> discovered = new ArrayList<>();
        
        for (String[] companyData : SEED_COMPANIES) {
            try {
                Company company = registerCompany(companyData[0], companyData[1]);
                if (company != null) {
                    company.setIndustry(companyData[2]);
                    company.setIsStartup(isStartupCompany(companyData[0]));
                    companyRepository.save(company);
                    discovered.add(company);
                }
            } catch (Exception e) {
                System.err.println("Error registering company " + companyData[0] + ": " + e.getMessage());
            }
        }
        
        return discovered;
    }
    
    /**
     * Discovers companies from startup directories (simplified - would need actual scraping in production)
     */
    public List<Company> discoverCompaniesFromStartupDirectories() {
        List<Company> discovered = new ArrayList<>();
        // This would scrape from YourStory, Crunchbase India, etc.
        // For now, return empty list - can be enhanced later
        return discovered;
    }
    
    /**
     * Discovers companies from job portals by extracting company names
     */
    public List<Company> discoverCompaniesFromJobPortals() {
        List<Company> discovered = new ArrayList<>();
        // This would extract company names from job portal listings
        // For now, return empty list - can be enhanced later
        return discovered;
    }
    
    /**
     * Registers a company in the database, detecting the career page platform
     */
    public Company registerCompany(String name, String careerPageUrl) {
        if (name == null || name.isBlank() || careerPageUrl == null || careerPageUrl.isBlank()) {
            return null;
        }
        
        // Check if company already exists
        var existing = companyRepository.findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Check by URL
        var existingByUrl = companyRepository.findByCareerPageUrl(careerPageUrl);
        if (existingByUrl.isPresent()) {
            return existingByUrl.get();
        }
        
        Company company = new Company();
        company.setName(name);
        company.setCareerPageUrl(careerPageUrl);
        company.setPlatform(detectPlatform(careerPageUrl));
        company.setIsActive(true);
        company.setCreatedAt(LocalDateTime.now());
        
        return company;
    }
    
    /**
     * Updates company scraping status
     */
    public void updateCompanyScrapingStatus(Long companyId, boolean isActive) {
        companyRepository.findById(companyId).ifPresent(company -> {
            company.setIsActive(isActive);
            company.setLastScraped(LocalDateTime.now());
            companyRepository.save(company);
        });
    }
    
    /**
     * Detects the career page platform from URL
     */
    private CareerPagePlatform detectPlatform(String url) {
        if (url == null || url.isBlank()) {
            return CareerPagePlatform.CUSTOM;
        }
        
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("lever.co") || lowerUrl.contains("jobs.lever.co")) {
            return CareerPagePlatform.LEVER;
        } else if (lowerUrl.contains("greenhouse.io") || lowerUrl.contains("boards.greenhouse.io")) {
            return CareerPagePlatform.GREENHOUSE;
        } else if (lowerUrl.contains("workable.com") || lowerUrl.contains("apply.workable.com")) {
            return CareerPagePlatform.WORKABLE;
        } else if (lowerUrl.contains("smartrecruiters.com")) {
            return CareerPagePlatform.SMARTRECRUITERS;
        } else if (lowerUrl.contains("bamboohr.com")) {
            return CareerPagePlatform.BAMBOOHR;
        } else {
            return CareerPagePlatform.CUSTOM;
        }
    }
    
    /**
     * Determines if a company is a startup (based on name patterns)
     */
    private Boolean isStartupCompany(String name) {
        // Simple heuristic - can be enhanced
        String[] startupIndicators = {"startup", "tech", "labs", "ventures", "innovations"};
        String lowerName = name.toLowerCase();
        for (String indicator : startupIndicators) {
            if (lowerName.contains(indicator)) {
                return true;
            }
        }
        // Check against known startups list
        String[] knownStartups = {"zomato", "swiggy", "razorpay", "phonepe", "paytm", 
                                   "byju", "unacademy", "cred", "meesho", "nykaa", 
                                   "policybazaar", "groww", "dream11", "sharechat", 
                                   "dailyhunt", "inmobi", "freshworks", "browserstack"};
        for (String startup : knownStartups) {
            if (lowerName.contains(startup)) {
                return true;
            }
        }
        return false;
    }
}

