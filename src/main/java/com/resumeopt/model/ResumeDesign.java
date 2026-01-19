package com.resumeopt.model;

/**
 * Enum representing different ATS-friendly resume design templates.
 * Each design is optimized for Applicant Tracking System parsing while maintaining visual appeal.
 */
public enum ResumeDesign {
    /**
     * MINIMAL - Clean, simple design with maximum ATS compatibility
     * Best for: Technical roles, entry-level positions, maximum ATS parsing
     * ATS Compatibility: 100%
     */
    MINIMAL("Minimal", 
            "Clean and simple design optimized for maximum ATS compatibility. Single column layout with minimal styling.",
            100, 
            "Technical roles, Entry-level positions, Maximum ATS parsing"),
    
    /**
     * PROFESSIONAL - Traditional corporate-friendly design
     * Best for: Corporate roles, traditional industries, business positions
     * ATS Compatibility: 95%
     */
    PROFESSIONAL("Professional", 
                 "Traditional corporate design with clear sections and professional formatting. Ideal for business and corporate roles.",
                 95, 
                 "Corporate roles, Business positions, Traditional industries"),
    
    /**
     * MODERN - Contemporary design with subtle styling
     * Best for: Modern companies, tech startups, contemporary roles
     * ATS Compatibility: 90%
     */
    MODERN("Modern", 
           "Contemporary design with subtle styling and balanced layout. Perfect for modern companies and tech roles.",
           90, 
           "Tech startups, Modern companies, Contemporary roles"),
    
    /**
     * CREATIVE - Unique layout for creative/design roles
     * Best for: Design roles, creative positions, marketing, UI/UX
     * ATS Compatibility: 85%
     */
    CREATIVE("Creative", 
             "Unique layout with creative typography and visual hierarchy. Designed for creative and design-focused roles.",
             85, 
             "Design roles, Creative positions, Marketing, UI/UX"),
    
    /**
     * EXECUTIVE - Sophisticated layout for senior positions
     * Best for: Executive roles, senior management, leadership positions
     * ATS Compatibility: 95%
     */
    EXECUTIVE("Executive", 
              "Sophisticated design with premium fonts and executive summary emphasis. Ideal for senior and leadership roles.",
              95, 
              "Executive roles, Senior management, Leadership positions"),
    
    /**
     * FRESHER - Optimized for entry-level positions with focus on education and projects
     * Best for: New graduates, freshers, entry-level technical roles
     * ATS Compatibility: 98%
     */
    FRESHER("Fresher", 
            "Optimized for entry-level positions with emphasis on education, projects, and internships. Perfect for new graduates and freshers.",
            98, 
            "New graduates, Entry-level positions, Technical roles, Academic achievements");
    
    private final String displayName;
    private final String description;
    private final int atsCompatibilityScore;
    private final String bestFor;
    
    ResumeDesign(String displayName, String description, int atsCompatibilityScore, String bestFor) {
        this.displayName = displayName;
        this.description = description;
        this.atsCompatibilityScore = atsCompatibilityScore;
        this.bestFor = bestFor;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getAtsCompatibilityScore() {
        return atsCompatibilityScore;
    }
    
    public String getBestFor() {
        return bestFor;
    }
    
    /**
     * Parse design from string name (case-insensitive)
     */
    public static ResumeDesign fromString(String name) {
        if (name == null || name.isBlank()) {
            return MINIMAL; // Default
        }
        
        String upperName = name.toUpperCase().trim();
        for (ResumeDesign design : values()) {
            if (design.name().equals(upperName) || design.displayName.toUpperCase().equals(upperName)) {
                return design;
            }
        }
        
        return MINIMAL; // Default fallback
    }
}

