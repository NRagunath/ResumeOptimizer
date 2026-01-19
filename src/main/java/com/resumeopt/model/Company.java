package com.resumeopt.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String careerPageUrl;
    
    @Enumerated(EnumType.STRING)
    private CareerPagePlatform platform;
    
    private Boolean isStartup;
    
    private String industry;
    
    private LocalDateTime lastScraped;
    
    private Boolean isActive = true;
    
    private Boolean isVerified = false;
    
    private Boolean isFresherFriendly = false;
    
    private Boolean isITSoftware = false;
    
    private Boolean isHighVolumeHiring = false;
    
    private Integer jobCount = 0;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCareerPageUrl() { return careerPageUrl; }
    public void setCareerPageUrl(String careerPageUrl) { this.careerPageUrl = careerPageUrl; }
    
    public CareerPagePlatform getPlatform() { return platform; }
    public void setPlatform(CareerPagePlatform platform) { this.platform = platform; }
    
    public Boolean getIsStartup() { return isStartup; }
    public void setIsStartup(Boolean isStartup) { this.isStartup = isStartup; }
    
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    
    public LocalDateTime getLastScraped() { return lastScraped; }
    public void setLastScraped(LocalDateTime lastScraped) { this.lastScraped = lastScraped; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    
    public Boolean getIsFresherFriendly() { return isFresherFriendly; }
    public void setIsFresherFriendly(Boolean isFresherFriendly) { this.isFresherFriendly = isFresherFriendly; }
    
    public Boolean getIsITSoftware() { return isITSoftware; }
    public void setIsITSoftware(Boolean isITSoftware) { this.isITSoftware = isITSoftware; }
    
    public Boolean getIsHighVolumeHiring() { return isHighVolumeHiring; }
    public void setIsHighVolumeHiring(Boolean isHighVolumeHiring) { this.isHighVolumeHiring = isHighVolumeHiring; }
    
    public Integer getJobCount() { return jobCount; }
    public void setJobCount(Integer jobCount) { this.jobCount = jobCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}

