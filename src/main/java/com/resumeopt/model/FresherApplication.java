package com.resumeopt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fresher_applications")
public class FresherApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_title")
    private String jobTitle;
    
    @Column(name = "company_name")
    private String companyName;
    
    @Column(name = "job_url", length = 2000)
    private String jobUrl;
    
    @Column(name = "application_status")
    private String applicationStatus; // Applied, Interview, Rejected, Accepted, etc.
    
    @Column(name = "application_date")
    private LocalDateTime applicationDate;
    
    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate;
    
    @Column(name = "interview_stage")
    private String interviewStage; // Phone Screen, Technical Interview, HR Interview, etc.
    
    @Column(name = "notes", length = 2000)
    private String notes;
    
    @Column(name = "is_campus_recruiting")
    private Boolean isCampusRecruiting = false;
    
    @Column(name = "is_internship_to_fulltime")
    private Boolean isInternshipToFulltime = false;
    
    @Column(name = "expected_salary")
    private String expectedSalary;
    
    @Column(name = "salary_offered")
    private String salaryOffered;
    
    @Column(name = "application_source")
    private String applicationSource; // Job portal, Referral, Campus, etc.
    
    @Column(name = "fresher_friendly_score")
    private Integer fresherFriendlyScore; // 1-10 scale rating for how fresher-friendly the company is
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public FresherApplication() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate;
    }

    public LocalDateTime getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDateTime followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getInterviewStage() {
        return interviewStage;
    }

    public void setInterviewStage(String interviewStage) {
        this.interviewStage = interviewStage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getIsCampusRecruiting() {
        return isCampusRecruiting;
    }

    public void setIsCampusRecruiting(Boolean campusRecruiting) {
        isCampusRecruiting = campusRecruiting;
    }

    public Boolean getIsInternshipToFulltime() {
        return isInternshipToFulltime;
    }

    public void setIsInternshipToFulltime(Boolean internshipToFulltime) {
        isInternshipToFulltime = internshipToFulltime;
    }

    public String getExpectedSalary() {
        return expectedSalary;
    }

    public void setExpectedSalary(String expectedSalary) {
        this.expectedSalary = expectedSalary;
    }

    public String getSalaryOffered() {
        return salaryOffered;
    }

    public void setSalaryOffered(String salaryOffered) {
        this.salaryOffered = salaryOffered;
    }

    public String getApplicationSource() {
        return applicationSource;
    }

    public void setApplicationSource(String applicationSource) {
        this.applicationSource = applicationSource;
    }

    public Integer getFresherFriendlyScore() {
        return fresherFriendlyScore;
    }

    public void setFresherFriendlyScore(Integer fresherFriendlyScore) {
        this.fresherFriendlyScore = fresherFriendlyScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}