package com.resumeopt.repo;

import com.resumeopt.model.ResumeChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResumeChangeRepository extends JpaRepository<ResumeChange, Long> {
    List<ResumeChange> findByResumeIdOrderByStartPositionAsc(Long resumeId);
    
    @Query("SELECT c FROM ResumeChange c WHERE c.resume.id = :resumeId ORDER BY c.startPosition ASC")
    List<ResumeChange> findAllByResumeId(@Param("resumeId") Long resumeId);
    
    List<ResumeChange> findByResumeIdAndStatus(Long resumeId, ResumeChange.ChangeStatus status);
    
    long countByResumeIdAndStatus(Long resumeId, ResumeChange.ChangeStatus status);
}

