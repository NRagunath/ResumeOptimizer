package com.resumeopt.repo;

import com.resumeopt.model.JobListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobListingRepository extends JpaRepository<JobListing, Long> {
    // Find existing jobs by apply URL (returns list since there can be duplicates)
    List<JobListing> findByApplyUrl(String applyUrl);
    
    // Find first job by apply URL (for quick duplicate check)
    @Query(value = "SELECT * FROM job_listing WHERE apply_url = :applyUrl ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    List<JobListing> findFirstByApplyUrlOrderByCreatedAtDescList(@Param("applyUrl") String applyUrl);
    
    // Find jobs by title and company (for duplicate detection)
    List<JobListing> findByTitleAndCompany(String title, String company);
    
    // Find recent jobs (last 7 days) for quick duplicate checking
    @Query("SELECT j FROM JobListing j WHERE j.createdAt IS NOT NULL AND j.createdAt >= :sevenDaysAgo ORDER BY j.createdAt DESC")
    List<JobListing> findRecentJobs(@Param("sevenDaysAgo") java.time.LocalDateTime sevenDaysAgo);
    
    // Alternative: Find all jobs if recent query fails
    List<JobListing> findAllByOrderByCreatedAtDesc();
}