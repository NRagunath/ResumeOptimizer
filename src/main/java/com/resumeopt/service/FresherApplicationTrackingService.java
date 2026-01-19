package com.resumeopt.service;

import com.resumeopt.model.FresherApplication;
import com.resumeopt.repo.FresherApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FresherApplicationTrackingService {
    
    @Autowired
    private FresherApplicationRepository fresherApplicationRepository;
    
    public FresherApplication saveApplication(FresherApplication application) {
        if (application.getId() == null) {
            application.setCreatedAt(LocalDateTime.now());
        }
        application.setUpdatedAt(LocalDateTime.now());
        return fresherApplicationRepository.save(application);
    }
    
    public Optional<FresherApplication> getApplicationById(Long id) {
        return fresherApplicationRepository.findById(id);
    }
    
    public List<FresherApplication> getAllApplications() {
        return fresherApplicationRepository.findAll();
    }
    
    public List<FresherApplication> getApplicationsByStatus(String status) {
        return fresherApplicationRepository.findByApplicationStatusOrderByApplicationDateDesc(status);
    }
    
    public List<FresherApplication> getCampusRecruitingApplications() {
        return fresherApplicationRepository.findByIsCampusRecruitingTrue();
    }
    
    public List<FresherApplication> getInternshipToFulltimeApplications() {
        return fresherApplicationRepository.findByIsInternshipToFulltimeTrue();
    }
    
    public List<FresherApplication> getApplicationsWithHighFresherScore(Integer minScore) {
        return fresherApplicationRepository.findByFresherFriendlyScoreGreaterThanEqual(minScore);
    }
    
    public void deleteApplication(Long id) {
        fresherApplicationRepository.deleteById(id);
    }
    
    public List<FresherApplication> getApplicationsInLast30Days() {
        return fresherApplicationRepository.findApplicationsInLast30Days();
    }
}