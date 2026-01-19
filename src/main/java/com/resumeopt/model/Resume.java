package com.resumeopt.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Lob
    private String originalText;

    @Lob
    private String optimizedText;

    @Lob
    private String finalApprovedText; // Final text after user accepts/declines changes

    private Double atsOriginalScore;
    private Double atsOptimizedScore;

    @Lob
    private byte[] optimizedPdf;

    @Lob
    private byte[] optimizedDocx; // template-preserved optimized resume output

    @Lob
    private byte[] originalFile; // original uploaded file (PDF or DOCX) to preserve template

    @Lob
    private String changeLogText; // textual change log of modifications

    private String originalFilename;
    private String contentType;
    
    @Enumerated(EnumType.STRING)
    private ResumeDesign selectedDesign;
    
    @Enumerated(EnumType.STRING)
    private ResumeDesign recommendedDesign;
    
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumeChange> changes;
    
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public String getOptimizedText() { return optimizedText; }
    public void setOptimizedText(String optimizedText) { this.optimizedText = optimizedText; }
    public Double getAtsOriginalScore() { return atsOriginalScore; }
    public void setAtsOriginalScore(Double atsOriginalScore) { this.atsOriginalScore = atsOriginalScore; }
    public Double getAtsOptimizedScore() { return atsOptimizedScore; }
    public void setAtsOptimizedScore(Double atsOptimizedScore) { this.atsOptimizedScore = atsOptimizedScore; }
    public byte[] getOptimizedPdf() { return optimizedPdf; }
    public void setOptimizedPdf(byte[] optimizedPdf) { this.optimizedPdf = optimizedPdf; }
    public byte[] getOptimizedDocx() { return optimizedDocx; }
    public void setOptimizedDocx(byte[] optimizedDocx) { this.optimizedDocx = optimizedDocx; }
    public byte[] getOriginalFile() { return originalFile; }
    public void setOriginalFile(byte[] originalFile) { this.originalFile = originalFile; }
    public String getChangeLogText() { return changeLogText; }
    public void setChangeLogText(String changeLogText) { this.changeLogText = changeLogText; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public ResumeDesign getSelectedDesign() { return selectedDesign; }
    public void setSelectedDesign(ResumeDesign selectedDesign) { this.selectedDesign = selectedDesign; }
    public ResumeDesign getRecommendedDesign() { return recommendedDesign; }
    public void setRecommendedDesign(ResumeDesign recommendedDesign) { this.recommendedDesign = recommendedDesign; }
    public String getFinalApprovedText() { return finalApprovedText; }
    public void setFinalApprovedText(String finalApprovedText) { this.finalApprovedText = finalApprovedText; }
    public List<ResumeChange> getChanges() { return changes; }
    public void setChanges(List<ResumeChange> changes) { this.changes = changes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}