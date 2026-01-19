package com.resumeopt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class ResumeChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    @JsonIgnore
    private Resume resume;

    @Enumerated(EnumType.STRING)
    private ChangeType changeType;

    @Lob
    private String originalText; // Text that was removed or changed

    @Lob
    private String newText; // Text that was added or changed to

    private Integer startPosition; // Start position in original text
    private Integer endPosition; // End position in original text
    private Integer newStartPosition; // Start position in optimized text
    private Integer newEndPosition; // End position in optimized text

    private String section; // Section name (e.g., "Skills", "Experience", "Education")

    @Enumerated(EnumType.STRING)
    private ChangeStatus status = ChangeStatus.PENDING;

    @Lob
    private String description; // Human-readable description of the change

    private Instant createdAt = Instant.now();

    public enum ChangeType {
        INSERT,    // New text added
        DELETE,    // Text removed
        MODIFY     // Text changed
    }

    public enum ChangeStatus {
        PENDING,   // Not yet reviewed
        ACCEPTED,  // User accepted the change
        DECLINED   // User declined the change
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public String getNewText() { return newText; }
    public void setNewText(String newText) { this.newText = newText; }
    public Integer getStartPosition() { return startPosition; }
    public void setStartPosition(Integer startPosition) { this.startPosition = startPosition; }
    public Integer getEndPosition() { return endPosition; }
    public void setEndPosition(Integer endPosition) { this.endPosition = endPosition; }
    public Integer getNewStartPosition() { return newStartPosition; }
    public void setNewStartPosition(Integer newStartPosition) { this.newStartPosition = newStartPosition; }
    public Integer getNewEndPosition() { return newEndPosition; }
    public void setNewEndPosition(Integer newEndPosition) { this.newEndPosition = newEndPosition; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public ChangeStatus getStatus() { return status; }
    public void setStatus(ChangeStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

