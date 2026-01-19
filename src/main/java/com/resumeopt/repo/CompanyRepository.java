package com.resumeopt.repo;

import com.resumeopt.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
    Optional<Company> findByCareerPageUrl(String careerPageUrl);
    List<Company> findByIsActiveTrue();
    List<Company> findByIsStartupTrue();
    List<Company> findByIsVerifiedTrue();
    List<Company> findByIsFresherFriendlyTrue();
    List<Company> findByIsITSoftwareTrue();
    List<Company> findByIsHighVolumeHiringTrue();
    List<Company> findByIsFresherFriendlyTrueAndIsITSoftwareTrue();
    List<Company> findByPlatform(com.resumeopt.model.CareerPagePlatform platform);
    List<Company> findByIndustryContainingIgnoreCase(String industry);
}

