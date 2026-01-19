package com.resumeopt.model;

import java.util.List;
import java.util.Set;

public class SkillsGapAnalysisResult {
    private Set<String> missingSkills;
    private Set<String> matchingSkills;
    private List<String> recommendations;
    private List<String> certificationSuggestions;

    public SkillsGapAnalysisResult(Set<String> missingSkills, Set<String> matchingSkills, 
                                   List<String> recommendations, List<String> certificationSuggestions) {
        this.missingSkills = missingSkills;
        this.matchingSkills = matchingSkills;
        this.recommendations = recommendations;
        this.certificationSuggestions = certificationSuggestions;
    }

    // Getters and setters
    public Set<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(Set<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public Set<String> getMatchingSkills() {
        return matchingSkills;
    }

    public void setMatchingSkills(Set<String> matchingSkills) {
        this.matchingSkills = matchingSkills;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public List<String> getCertificationSuggestions() {
        return certificationSuggestions;
    }

    public void setCertificationSuggestions(List<String> certificationSuggestions) {
        this.certificationSuggestions = certificationSuggestions;
    }
}