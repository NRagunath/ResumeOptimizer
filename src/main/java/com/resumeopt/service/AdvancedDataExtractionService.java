package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced data extraction service for parsing salary, location, deadlines, and other metadata
 */
@Service
public class AdvancedDataExtractionService {
    
    // Salary patterns (Indian format)
    private static final Pattern SALARY_PATTERN = Pattern.compile(
        "(?i)(?:salary|ctc|package|stipend|compensation|pay).*?(?:\\d+(?:\\.\\d+)?)\\s*(?:lpa|lakh|lakhs|cr|crore|crores|k|thousand|per\\s*month|pm|p\\.a\\.|per\\s*annum|pa)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern SALARY_RANGE_PATTERN = Pattern.compile(
        "(\\d+(?:\\.\\d+)?)\\s*(?:to|-|–|—)\\s*(\\d+(?:\\.\\d+)?)\\s*(lpa|lakh|lakhs|cr|crore|k|thousand)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SINGLE_SALARY_PATTERN = Pattern.compile(
        "(\\d+(?:\\.\\d+)?)\\s*(lpa|lakh|lakhs|cr|crore|k|thousand)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Location patterns
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
        "(?i)(?:location|based\\s*in|work\\s*from|office\\s*at|hybrid|remote|on-site|onsite|wfh|work from home).*?([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final List<String> INDIAN_CITIES = List.of(
        "Bangalore", "Mumbai", "Delhi", "Hyderabad", "Chennai", "Pune", "Kolkata",
        "Ahmedabad", "Jaipur", "Surat", "Lucknow", "Kanpur", "Nagpur", "Indore",
        "Thane", "Bhopal", "Visakhapatnam", "Patna", "Vadodara", "Ghaziabad"
    );
    
    // Deadline patterns
    private static final Pattern DEADLINE_PATTERN = Pattern.compile(
        "(?i)(?:deadline|last\\s*date|apply\\s*by|closing\\s*date|expires?|valid\\s*until).*?(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4})",
        Pattern.CASE_INSENSITIVE
    );
    
    // Experience patterns
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile(
        "(?i)(?:experience|exp|years?|yrs?|entry\\s*level|0-1|0\\s*to\\s*1|1\\+?|2\\+?|3\\+?).*?(\\d+\\s*(?:to|-|–|—)?\\s*\\d*\\s*(?:years?|yrs?|months?))",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Extract and enrich job listing with advanced data extraction
     */
    public void enrichJobListing(JobListing job) {
        if (job == null) {
            return;
        }
        
        String description = job.getDescription() != null ? job.getDescription() : "";
        String title = job.getTitle() != null ? job.getTitle() : "";
        String combined = title + " " + description;
        
        // Extract salary
        if (job.getSalaryRange() == null || job.getSalaryRange().isBlank()) {
            String salary = extractSalary(combined);
            if (salary != null && !salary.isBlank()) {
                job.setSalaryRange(salary);
            }
        }
        
        // Extract location
        if (job.getLocation() == null || job.getLocation().isBlank()) {
            String location = extractLocation(combined);
            if (location != null && !location.isBlank()) {
                job.setLocation(location);
            }
        }
        
        // Extract deadline
        if (job.getApplicationDeadline() == null) {
            LocalDateTime deadline = extractDeadline(combined);
            if (deadline != null) {
                job.setApplicationDeadline(deadline);
            }
        }
        
        // Extract experience required
        if (job.getExperienceRequired() == null) {
            Integer experience = extractExperience(combined);
            if (experience != null) {
                job.setExperienceRequired(experience);
            }
        }
    }
    
    /**
     * Extract salary range from text
     */
    public String extractSalary(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        
        // Try range pattern first
        Matcher rangeMatcher = SALARY_RANGE_PATTERN.matcher(text);
        if (rangeMatcher.find()) {
            String min = rangeMatcher.group(1);
            String max = rangeMatcher.group(2);
            String unit = rangeMatcher.group(3).toLowerCase();
            
            double minVal = Double.parseDouble(min);
            double maxVal = Double.parseDouble(max);
            
            // Normalize to LPA
            if (unit.contains("k") || unit.contains("thousand")) {
                minVal = minVal / 100;
                maxVal = maxVal / 100;
            } else if (unit.contains("cr") || unit.contains("crore")) {
                minVal = minVal * 100;
                maxVal = maxVal * 100;
            }
            
            return String.format("%.1f - %.1f LPA", minVal, maxVal);
        }
        
        // Try single salary
        Matcher singleMatcher = SINGLE_SALARY_PATTERN.matcher(text);
        if (singleMatcher.find()) {
            String amount = singleMatcher.group(1);
            String unit = singleMatcher.group(2).toLowerCase();
            
            double value = Double.parseDouble(amount);
            
            if (unit.contains("k") || unit.contains("thousand")) {
                value = value / 100;
            } else if (unit.contains("cr") || unit.contains("crore")) {
                value = value * 100;
            }
            
            return String.format("%.1f LPA", value);
        }
        
        // Try general salary pattern
        Matcher generalMatcher = SALARY_PATTERN.matcher(text);
        if (generalMatcher.find()) {
            String match = generalMatcher.group(0);
            // Extract numbers from match
            Pattern numPattern = Pattern.compile("\\d+(?:\\.\\d+)?");
            Matcher numMatcher = numPattern.matcher(match);
            List<String> numbers = new ArrayList<>();
            while (numMatcher.find()) {
                numbers.add(numMatcher.group());
            }
            if (!numbers.isEmpty()) {
                return numbers.get(0) + " LPA (approx)";
            }
        }
        
        return null;
    }
    
    /**
     * Extract location from text
     */
    public String extractLocation(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        
        // Check for common location keywords
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("remote") || lowerText.contains("work from home") || lowerText.contains("wfh")) {
            return "Remote";
        }
        
        if (lowerText.contains("hybrid")) {
            return "Hybrid";
        }
        
        // Try to find Indian cities
        for (String city : INDIAN_CITIES) {
            if (text.contains(city)) {
                return city;
            }
        }
        
        // Try pattern matching
        Matcher locationMatcher = LOCATION_PATTERN.matcher(text);
        if (locationMatcher.find()) {
            String location = locationMatcher.group(1);
            if (location != null && location.length() > 2 && location.length() < 50) {
                return location.trim();
            }
        }
        
        return null;
    }
    
    /**
     * Extract application deadline from text
     */
    public LocalDateTime extractDeadline(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        
        Matcher deadlineMatcher = DEADLINE_PATTERN.matcher(text);
        if (deadlineMatcher.find()) {
            String dateStr = deadlineMatcher.group(1).trim();
            
            // Try multiple date formats
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("dd-MM-yy"),
                DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(dateStr, formatter);
                } catch (DateTimeParseException e) {
                    // Try next format
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract experience required from text
     */
    public Integer extractExperience(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        
        String lowerText = text.toLowerCase();
        
        // Check for entry level
        if (lowerText.contains("entry level") || 
            lowerText.contains("0-1") || lowerText.contains("0 to 1") ||
            lowerText.contains("0 years")) {
            return 0;
        }
        
        // Try pattern matching
        Matcher expMatcher = EXPERIENCE_PATTERN.matcher(text);
        if (expMatcher.find()) {
            String expStr = expMatcher.group(1);
            
            // Extract numbers
            Pattern numPattern = Pattern.compile("(\\d+)");
            Matcher numMatcher = numPattern.matcher(expStr);
            
            if (numMatcher.find()) {
                int years = Integer.parseInt(numMatcher.group(1));
                return Math.min(years, 20); // Cap at 20 years
            }
        }
        
        return null;
    }
    
    /**
     * Extract data from HTML document
     */
    public void enrichFromDocument(JobListing job, Document doc) {
        if (job == null || doc == null) {
            return;
        }
        
        // Extract from meta tags
        Elements metaTags = doc.select("meta[property], meta[name]");
        for (Element meta : metaTags) {
            String property = meta.attr("property");
            String name = meta.attr("name");
            String content = meta.attr("content");
            
            if (content != null && !content.isBlank()) {
                if ((property.contains("salary") || name.contains("salary")) && 
                    job.getSalaryRange() == null) {
                    job.setSalaryRange(extractSalary(content));
                }
                
                if ((property.contains("location") || name.contains("location")) && 
                    job.getLocation() == null) {
                    job.setLocation(extractLocation(content));
                }
            }
        }
        
        // Extract from structured data (JSON-LD)
        Elements jsonLd = doc.select("script[type='application/ld+json']");
        for (Element script : jsonLd) {
            String json = script.html();
            // Parse JSON and extract job posting data
            // This would require a JSON parser - simplified here
            if (json.contains("baseSalary") && job.getSalaryRange() == null) {
                String salary = extractSalary(json);
                if (salary != null) {
                    job.setSalaryRange(salary);
                }
            }
        }
    }
}

