package com.resumeopt.service;

import com.resumeopt.model.Company;
import com.resumeopt.model.CareerPagePlatform;
import com.resumeopt.repo.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EnhancedCompanyDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedCompanyDiscoveryService.class);

    @Autowired
    private CompanyRepository companyRepository;

    // Fresher-friendly companies with their career page URLs
    private static final Map<String, String> FRESHER_FRIENDLY_COMPANIES = new HashMap<>();
    
    static {
        // Technology Companies
        FRESHER_FRIENDLY_COMPANIES.put("TCS", "https://ibegin.tcs.com/iBegin/");
        FRESHER_FRIENDLY_COMPANIES.put("Infosys", "https://career.infosys.com/joblist");
        FRESHER_FRIENDLY_COMPANIES.put("Wipro", "https://careers.wipro.com/careers-home/");
        FRESHER_FRIENDLY_COMPANIES.put("HCL Technologies", "https://www.hcltech.com/careers/careers-in-india");
        FRESHER_FRIENDLY_COMPANIES.put("Tech Mahindra", "https://careers.techmahindra.com/");
        FRESHER_FRIENDLY_COMPANIES.put("Cognizant", "https://careers.cognizant.com/global/en");
        FRESHER_FRIENDLY_COMPANIES.put("Accenture", "https://www.accenture.com/in-en/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Capgemini", "https://www.capgemini.com/in-en/careers/");
        FRESHER_FRIENDLY_COMPANIES.put("IBM", "https://www.ibm.com/in-en/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Microsoft", "https://careers.microsoft.com/us/en");
        
        // Startups and Mid-size Companies (Using direct ATS links where possible)
        FRESHER_FRIENDLY_COMPANIES.put("Zomato", "https://jobs.lever.co/zomato");
        FRESHER_FRIENDLY_COMPANIES.put("Swiggy", "https://jobs.lever.co/swiggy");
        FRESHER_FRIENDLY_COMPANIES.put("Flipkart", "https://www.flipkartcareers.com/#!/");
        FRESHER_FRIENDLY_COMPANIES.put("Paytm", "https://jobs.lever.co/paytm");
        FRESHER_FRIENDLY_COMPANIES.put("Ola", "https://jobs.lever.co/olacabs");
        FRESHER_FRIENDLY_COMPANIES.put("Byju's", "https://jobs.lever.co/byjus");
        FRESHER_FRIENDLY_COMPANIES.put("Unacademy", "https://jobs.lever.co/unacademy");
        FRESHER_FRIENDLY_COMPANIES.put("Razorpay", "https://jobs.lever.co/razorpay");
        FRESHER_FRIENDLY_COMPANIES.put("Freshworks", "https://careers.freshworks.com/");
        FRESHER_FRIENDLY_COMPANIES.put("Zoho", "https://www.zoho.com/careers/");
        
        // Banking and Financial Services
        FRESHER_FRIENDLY_COMPANIES.put("HDFC Bank", "https://www.hdfcbank.com/personal/about-us/careers");
        FRESHER_FRIENDLY_COMPANIES.put("ICICI Bank", "https://www.icicibank.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Axis Bank", "https://www.axisbank.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("SBI", "https://bank.sbi/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Kotak Mahindra Bank", "https://www.kotak.com/en/careers");
        
        // E-commerce and Retail
        FRESHER_FRIENDLY_COMPANIES.put("Amazon", "https://www.amazon.jobs");
        FRESHER_FRIENDLY_COMPANIES.put("Myntra", "https://careers.myntra.com");
        FRESHER_FRIENDLY_COMPANIES.put("Nykaa", "https://www.nykaa.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("BigBasket", "https://www.bigbasket.com/careers");
        
        // Consulting and Services
        FRESHER_FRIENDLY_COMPANIES.put("Deloitte", "https://www2.deloitte.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("EY", "https://www.ey.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("PwC", "https://www.pwc.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("KPMG", "https://home.kpmg/careers");
        
        // Telecommunications
        FRESHER_FRIENDLY_COMPANIES.put("Jio", "https://www.jio.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Airtel", "https://www.airtel.in/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Vodafone Idea", "https://www.vodafoneidea.com/careers");
        
        // Manufacturing and Automotive
        FRESHER_FRIENDLY_COMPANIES.put("Tata Motors", "https://www.tatamotors.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Mahindra", "https://www.mahindra.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Maruti Suzuki", "https://www.marutisuzuki.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Hero MotoCorp", "https://www.heromotocorp.com/careers");
        
        // Healthcare and Pharmaceuticals
        FRESHER_FRIENDLY_COMPANIES.put("Apollo Hospitals", "https://www.apollohospitals.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Fortis Healthcare", "https://www.fortishealthcare.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Dr. Reddy's", "https://www.drreddys.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Cipla", "https://www.cipla.com/careers");
        
        // Media and Entertainment
        FRESHER_FRIENDLY_COMPANIES.put("Hotstar", "https://www.hotstar.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Zee Entertainment", "https://www.zeeentertainment.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("Sony Pictures", "https://www.sonypictures.com/careers");
        
        // Government and Public Sector
        FRESHER_FRIENDLY_COMPANIES.put("ISRO", "https://www.isro.gov.in/careers");
        FRESHER_FRIENDLY_COMPANIES.put("DRDO", "https://www.drdo.gov.in/careers");
        FRESHER_FRIENDLY_COMPANIES.put("BHEL", "https://www.bhel.com/careers");
        FRESHER_FRIENDLY_COMPANIES.put("ONGC", "https://www.ongcindia.com/careers");
    }


    /**
     * Discovers all companies
     */
    public List<Company> discoverAllCompanies() {
        return companyRepository.findAll();
    }


    /**
     * Discovers IT and software companies
     */
    public List<Company> discoverITAndSoftwareCompanies() {
        return companyRepository.findByIsITSoftwareTrue();
    }



    /**
     * Gets high volume hiring companies
     */
    public List<Company> getHighVolumeHiringCompanies() {
        return companyRepository.findByIsHighVolumeHiringTrue();
    }

    /**
     * Gets companies by industry
     */
    public List<Company> getCompaniesByIndustry(String industry) {
        return companyRepository.findByIndustryContainingIgnoreCase(industry);
    }

    /**
     * Gets active companies
     */
    public List<Company> getActiveCompanies() {
        return companyRepository.findByIsActiveTrue();
    }

    /**
     * Gets startup companies
     */
    public List<Company> getStartupCompanies() {
        return companyRepository.findByIsStartupTrue();
    }

    /**
     * Gets verified companies
     */
    public List<Company> getVerifiedCompanies() {
        return companyRepository.findByIsVerifiedTrue();
    }

    /**
     * Gets companies by platform
     */
    public List<Company> getCompaniesByPlatform(CareerPagePlatform platform) {
        return companyRepository.findByPlatform(platform);
    }

    /**
     * Searches companies by name (using existing method)
     */
    public Optional<Company> searchCompanyByName(String name) {
        return companyRepository.findByName(name);
    }

    /**
     * Gets all companies with pagination support
     */
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    /**
     * Updates company scraping status
     */
    public void updateCompanyScrapingStatus(Long companyId, Boolean isActive, int jobCount) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isPresent()) {
            Company company = companyOpt.get();
            company.setIsActive(isActive);
            company.setJobCount(jobCount);
            company.setLastScraped(LocalDateTime.now());
            company.setLastUpdated(LocalDateTime.now());
            companyRepository.save(company);
            logger.debug("Updated scraping status for company: {} - Active: {}, Jobs: {}", 
                        company.getName(), isActive, jobCount);
        }
    }

    /**
     * Validates and updates company URLs
     */
    public void validateAndUpdateCompanyUrls() {
        logger.info("Starting validation and update of company URLs...");
        List<Company> allCompanies = companyRepository.findAll();
        int updatedCount = 0;
        
        for (Company company : allCompanies) {
            try {
                // Basic URL validation and cleanup
                String url = company.getCareerPageUrl();
                if (url != null && !url.trim().isEmpty()) {
                    // Ensure URL has proper protocol
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                        company.setCareerPageUrl(url);
                        company.setLastUpdated(LocalDateTime.now());
                        companyRepository.save(company);
                        updatedCount++;
                        logger.debug("Updated URL for company: {} - New URL: {}", company.getName(), url);
                    }
                }
            } catch (Exception e) {
                logger.error("Error validating URL for company {}: {}", company.getName(), e.getMessage());
            }
        }
        
        logger.info("URL validation completed. Updated {} companies", updatedCount);
    }

    /**
     * Registers a new company
     */
    public Company registerCompany(String name, String careerPageUrl) {
        logger.info("Registering new company: {}", name);
        
        // Check if company already exists
        Optional<Company> existingCompany = companyRepository.findByName(name);
        if (existingCompany.isPresent()) {
            logger.warn("Company already exists: {}", name);
            return existingCompany.get();
        }
        
        Company company = new Company();
        company.setName(name);
        company.setCareerPageUrl(careerPageUrl);
        company.setIndustry(detectIndustry(name));
        company.setPlatform(detectPlatform(careerPageUrl));
        company.setIsActive(true);
        company.setIsFresherFriendly(false); // Default to false, can be updated later
        company.setCreatedAt(LocalDateTime.now());
        company.setLastUpdated(LocalDateTime.now());
        
        Company savedCompany = companyRepository.save(company);
        logger.info("Successfully registered company: {}", name);
        return savedCompany;
    }

    /**
     * Detects the industry based on company name
     */
    private String detectIndustry(String companyName) {
        String lowerName = companyName.toLowerCase();
        
        if (lowerName.contains("tech") || lowerName.contains("software") || 
            Arrays.asList("tcs", "infosys", "wipro", "hcl", "microsoft", "ibm", "cognizant", "accenture").contains(lowerName)) {
            return "Technology";
        } else if (lowerName.contains("bank") || Arrays.asList("hdfc", "icici", "axis", "sbi", "kotak").contains(lowerName)) {
            return "Banking & Financial Services";
        } else if (Arrays.asList("zomato", "swiggy", "flipkart", "amazon", "myntra", "nykaa").contains(lowerName)) {
            return "E-commerce & Retail";
        } else if (Arrays.asList("deloitte", "ey", "pwc", "kpmg").contains(lowerName)) {
            return "Consulting";
        } else if (Arrays.asList("jio", "airtel", "vodafone").contains(lowerName)) {
            return "Telecommunications";
        } else if (lowerName.contains("motor") || Arrays.asList("tata motors", "mahindra", "maruti", "hero").contains(lowerName)) {
            return "Automotive";
        } else if (lowerName.contains("hospital") || lowerName.contains("pharma") || 
                   Arrays.asList("apollo", "fortis", "cipla", "dr. reddy's").contains(lowerName)) {
            return "Healthcare & Pharmaceuticals";
        } else if (Arrays.asList("hotstar", "zee", "sony").contains(lowerName)) {
            return "Media & Entertainment";
        } else if (Arrays.asList("isro", "drdo", "bhel", "ongc").contains(lowerName)) {
            return "Government & Public Sector";
        } else {
            return "Other";
        }
    }

    /**
     * Detects the platform based on career URL
     */
    private CareerPagePlatform detectPlatform(String url) {
        String lowerUrl = url.toLowerCase();
        
        if (lowerUrl.contains("workday")) {
            return CareerPagePlatform.WORKDAY;
        } else if (lowerUrl.contains("greenhouse")) {
            return CareerPagePlatform.GREENHOUSE;
        } else if (lowerUrl.contains("lever")) {
            return CareerPagePlatform.LEVER;
        } else if (lowerUrl.contains("bamboohr")) {
            return CareerPagePlatform.BAMBOOHR;
        } else if (lowerUrl.contains("smartrecruiters")) {
            return CareerPagePlatform.SMARTRECRUITERS;
        } else if (lowerUrl.contains("jobvite")) {
            return CareerPagePlatform.JOBVITE;
        } else if (lowerUrl.contains("icims")) {
            return CareerPagePlatform.ICIMS;
        } else if (lowerUrl.contains("taleo")) {
            return CareerPagePlatform.TALEO;
        } else {
            return CareerPagePlatform.CUSTOM;
        }
    }
}