package com.resumeopt.repo;

import com.resumeopt.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /**
     * Find all applications whose applyUrl is in the given list.
     */
    List<JobApplication> findByApplyUrlIn(Iterable<String> applyUrls);
}