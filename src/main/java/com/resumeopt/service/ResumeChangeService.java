package com.resumeopt.service;

import com.resumeopt.model.Resume;
import com.resumeopt.model.ResumeChange;
import com.resumeopt.repo.ResumeChangeRepository;
import com.resumeopt.repo.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeChangeService {

    private final ResumeChangeRepository changeRepository;
    private final ResumeRepository resumeRepository;

    public ResumeChangeService(ResumeChangeRepository changeRepository, ResumeRepository resumeRepository) {
        this.changeRepository = changeRepository;
        this.resumeRepository = resumeRepository;
    }

    /**
     * Accept a change
     */
    @Transactional
    public ResumeChange acceptChange(Long changeId) {
        if (changeId == null) {
            throw new IllegalArgumentException("Change ID cannot be null");
        }
        ResumeChange change = changeRepository.findById(changeId)
            .orElseThrow(() -> new IllegalArgumentException("Change not found: " + changeId));
        change.setStatus(ResumeChange.ChangeStatus.ACCEPTED);
        return changeRepository.save(change);
    }

    /**
     * Decline a change
     */
    @Transactional
    public ResumeChange declineChange(Long changeId) {
        if (changeId == null) {
            throw new IllegalArgumentException("Change ID cannot be null");
        }
        ResumeChange change = changeRepository.findById(changeId)
            .orElseThrow(() -> new IllegalArgumentException("Change not found: " + changeId));
        change.setStatus(ResumeChange.ChangeStatus.DECLINED);
        return changeRepository.save(change);
    }

    /**
     * Get all changes for a resume
     */
    public List<ResumeChange> getChanges(Long resumeId) {
        return changeRepository.findByResumeIdOrderByStartPositionAsc(resumeId);
    }

    /**
     * Generate real-time preview based on accepted changes
     */
    public String generatePreview(Long resumeId) {
        if (resumeId == null) {
            throw new IllegalArgumentException("Resume ID cannot be null");
        }
        Resume resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));
        
        List<ResumeChange> changes = changeRepository.findByResumeIdOrderByStartPositionAsc(resumeId);
        String originalText = resume.getOriginalText() != null ? resume.getOriginalText() : "";
        
        // Build preview by applying only accepted changes
        return applyChanges(originalText, changes, true);
    }

    /**
     * Generate final resume text from accepted changes only
     */
    @Transactional
    public Resume finalizeResume(Long resumeId) {
        if (resumeId == null) {
            throw new IllegalArgumentException("Resume ID cannot be null");
        }
        Resume resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));
        
        List<ResumeChange> changes = changeRepository.findByResumeIdOrderByStartPositionAsc(resumeId);
        String originalText = resume.getOriginalText() != null ? resume.getOriginalText() : "";
        
        // Apply only accepted changes
        String finalText = applyChanges(originalText, changes, true);
        resume.setFinalApprovedText(finalText);
        
        return resumeRepository.save(resume);
    }

    /**
     * Apply changes to text
     * @param originalText The original text
     * @param changes List of changes to apply
     * @param onlyAccepted If true, only apply accepted changes; if false, apply all pending changes
     * @return Modified text
     */
    private String applyChanges(String originalText, List<ResumeChange> changes, boolean onlyAccepted) {
        if (changes.isEmpty()) {
            return originalText;
        }

        // Filter changes based on status
        List<ResumeChange> changesToApply = changes.stream()
            .filter(c -> onlyAccepted 
                ? c.getStatus() == ResumeChange.ChangeStatus.ACCEPTED
                : c.getStatus() != ResumeChange.ChangeStatus.DECLINED)
            .sorted((a, b) -> {
                // Sort by position in reverse order to apply from end to start
                int posA = a.getStartPosition() != null ? a.getStartPosition() : 0;
                int posB = b.getStartPosition() != null ? b.getStartPosition() : 0;
                return Integer.compare(posB, posA);
            })
            .collect(Collectors.toList());

        StringBuilder result = new StringBuilder(originalText);

        // Apply changes from end to start to preserve positions
        for (ResumeChange change : changesToApply) {
            applyChange(result, change);
        }

        return result.toString();
    }

    /**
     * Apply a single change to the text
     */
    private void applyChange(StringBuilder text, ResumeChange change) {
        if (change.getChangeType() == ResumeChange.ChangeType.DELETE) {
            // Delete text
            if (change.getStartPosition() != null && change.getEndPosition() != null) {
                int start = Math.min(change.getStartPosition(), text.length());
                int end = Math.min(change.getEndPosition(), text.length());
                if (start < end && start >= 0) {
                    text.delete(start, end);
                }
            }
        } else if (change.getChangeType() == ResumeChange.ChangeType.INSERT) {
            // Insert text
            if (change.getNewStartPosition() != null && change.getNewText() != null) {
                int pos = Math.min(change.getNewStartPosition(), text.length());
                if (pos >= 0) {
                    text.insert(pos, change.getNewText());
                }
            }
        } else if (change.getChangeType() == ResumeChange.ChangeType.MODIFY) {
            // Replace text
            if (change.getStartPosition() != null && change.getEndPosition() != null) {
                int start = Math.min(change.getStartPosition(), text.length());
                int end = Math.min(change.getEndPosition(), text.length());
                if (start < end && start >= 0 && change.getNewText() != null) {
                    text.replace(start, end, change.getNewText());
                }
            }
        }
    }

    /**
     * Get statistics about changes
     */
    public Map<String, Object> getChangeStatistics(Long resumeId) {
        List<ResumeChange> allChanges = changeRepository.findByResumeIdOrderByStartPositionAsc(resumeId);
        
        long total = allChanges.size();
        long accepted = changeRepository.countByResumeIdAndStatus(resumeId, ResumeChange.ChangeStatus.ACCEPTED);
        long declined = changeRepository.countByResumeIdAndStatus(resumeId, ResumeChange.ChangeStatus.DECLINED);
        long pending = changeRepository.countByResumeIdAndStatus(resumeId, ResumeChange.ChangeStatus.PENDING);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("accepted", accepted);
        stats.put("declined", declined);
        stats.put("pending", pending);
        
        // Count by type
        long inserts = allChanges.stream().filter(c -> c.getChangeType() == ResumeChange.ChangeType.INSERT).count();
        long deletes = allChanges.stream().filter(c -> c.getChangeType() == ResumeChange.ChangeType.DELETE).count();
        long modifies = allChanges.stream().filter(c -> c.getChangeType() == ResumeChange.ChangeType.MODIFY).count();
        
        stats.put("inserts", inserts);
        stats.put("deletes", deletes);
        stats.put("modifies", modifies);
        
        return stats;
    }
}

