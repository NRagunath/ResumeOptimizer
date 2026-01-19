package com.resumeopt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
public class JobListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String title;
    private String company;
    @Column(length = 5000)
    private String description;
    @Column(length = 1000)
    private String applyUrl;
    private Boolean linkVerified;
    
    private LocalDateTime postedDate;
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private MatchLevel matchLevel = MatchLevel.NOT_RECOMMENDED;
    
    @Enumerated(EnumType.STRING)
    private JobType jobType;
    
    @Enumerated(EnumType.STRING)
    private JobSource source;
    
    @Column(length = 1000)
    private String sourceCompanyUrl;
    
    private LocalDateTime applicationDeadline;
    
    private String location;
    
    private String salaryRange;
    
    private Integer experienceRequired;

    // Non-persistent computed attributes for entry-level analytics
    @Transient
    private Double successProbability;

    @Transient
    private java.util.List<String> requiredSkills;

    @Transient
    private java.util.List<String> skillGaps;

    @Transient
    private java.util.List<String> guidanceTips;
    
    @Transient
    private Integer fresherFriendlyScore;  // Score from 1-10 indicating how fresher-friendly the job is

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    public Boolean getLinkVerified() { return linkVerified; }
    public void setLinkVerified(Boolean linkVerified) { this.linkVerified = linkVerified; }
    public MatchLevel getMatchLevel() { return matchLevel; }
    public void setMatchLevel(MatchLevel matchLevel) { this.matchLevel = matchLevel; }

    public Double getSuccessProbability() { return successProbability; }
    public void setSuccessProbability(Double successProbability) { this.successProbability = successProbability; }
    public java.util.List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(java.util.List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    public java.util.List<String> getSkillGaps() { return skillGaps; }
    public void setSkillGaps(java.util.List<String> skillGaps) { this.skillGaps = skillGaps; }
    public java.util.List<String> getGuidanceTips() { return guidanceTips; }
    public void setGuidanceTips(java.util.List<String> guidanceTips) { this.guidanceTips = guidanceTips; }
    
    public LocalDateTime getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDateTime postedDate) { this.postedDate = postedDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }
    
    public JobSource getSource() { return source; }
    public void setSource(JobSource source) { this.source = source; }
    
    public String getSourceCompanyUrl() { return sourceCompanyUrl; }
    public void setSourceCompanyUrl(String sourceCompanyUrl) { this.sourceCompanyUrl = sourceCompanyUrl; }
    
    public LocalDateTime getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(LocalDateTime applicationDeadline) { this.applicationDeadline = applicationDeadline; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }
    
    public Integer getExperienceRequired() { return experienceRequired; }
    public void setExperienceRequired(Integer experienceRequired) { this.experienceRequired = experienceRequired; }
    
    public Integer getFresherFriendlyScore() { return fresherFriendlyScore; }
    public void setFresherFriendlyScore(Integer fresherFriendlyScore) { this.fresherFriendlyScore = fresherFriendlyScore; }
}