package com.resumeopt.repo;

import com.resumeopt.model.FresherApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FresherApplicationRepository extends JpaRepository<FresherApplication, Long> {
    List<FresherApplication> findByApplicationStatus(String status);
    
    List<FresherApplication> findByApplicationStatusOrderByApplicationDateDesc(String status);
    
    List<FresherApplication> findByIsCampusRecruitingTrue();
    
    List<FresherApplication> findByIsInternshipToFulltimeTrue();
    
    @Query("SELECT f FROM FresherApplication f WHERE f.companyName = :companyName")
    List<FresherApplication> findByCompanyName(@Param("companyName") String companyName);
    
    @Query(value = "SELECT * FROM fresher_applications WHERE application_date >= DATEADD('DAY', -30, CURRENT_DATE)", nativeQuery = true)
    List<FresherApplication> findApplicationsInLast30Days();
    
    @Query("SELECT f FROM FresherApplication f WHERE f.fresherFriendlyScore >= :minScore")
    List<FresherApplication> findByFresherFriendlyScoreGreaterThanEqual(@Param("minScore") Integer minScore);
}