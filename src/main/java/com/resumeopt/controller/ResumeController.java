package com.resumeopt.controller;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.resumeopt.model.Resume;
import com.resumeopt.model.ResumeDesign;
import com.resumeopt.model.ResumeTemplateStyle;
import com.resumeopt.model.StructuredResumeView;
import com.resumeopt.realtime.RealtimeEventPublisher;
import com.resumeopt.repo.ResumeRepository;
import com.resumeopt.service.ResumeDesignService;
import com.resumeopt.service.ResumeDocService;
import com.resumeopt.service.ResumeOptimizationService;
import com.resumeopt.service.ResumeParserService;
import com.resumeopt.service.ResumePdfService;
import com.resumeopt.service.ResumeDiffService;
import com.resumeopt.service.ResumeChangeService;
import com.resumeopt.service.ResumeStructuringService;
import com.resumeopt.service.SkillsGapAnalysisService;
import com.resumeopt.model.ResumeChange;
import com.resumeopt.repo.ResumeChangeRepository;

@Controller
@Validated
public class ResumeController {

    private final ResumeParserService parserService;
    private final ResumeOptimizationService optimizationService;
    private final ResumeRepository resumeRepository;
    private final RealtimeEventPublisher events;
    private final ResumePdfService pdfService;
    private final ResumeDocService docService;
    private final ResumeDesignService designService;
    private final ResumeDiffService diffService;
    private final ResumeChangeService changeService;
    private final ResumeChangeRepository changeRepository;
    private final ResumeStructuringService structuringService;
    private final SkillsGapAnalysisService skillsGapAnalysisService;

    public ResumeController(ResumeParserService parserService,
                            ResumeOptimizationService optimizationService,
                            ResumeRepository resumeRepository,
                            RealtimeEventPublisher events,
                            ResumePdfService pdfService,
                            ResumeDocService docService,
                            ResumeDesignService designService,
                            ResumeDiffService diffService,
                            ResumeChangeService changeService,
                            ResumeChangeRepository changeRepository,
                            ResumeStructuringService structuringService,
                            SkillsGapAnalysisService skillsGapAnalysisService) {
        this.parserService = parserService;
        this.optimizationService = optimizationService;
        this.resumeRepository = resumeRepository;
        this.events = events;
        this.pdfService = pdfService;
        this.docService = docService;
        this.designService = designService;
        this.diffService = diffService;
        this.changeService = changeService;
        this.changeRepository = changeRepository;
        this.structuringService = structuringService;
        this.skillsGapAnalysisService = skillsGapAnalysisService;
    }

    @PostMapping("/resume/skills-gap-analysis")
    @ResponseBody
    public java.util.Map<String, Object> analyzeSkillsGap(@RequestParam String resumeText, 
                                                  @RequestParam String jobDescription, 
                                                  @RequestParam Long jobId) {
        try {
            // Get the job listing from the database or service
            // For now, we'll create a basic job listing object
            com.resumeopt.model.JobListing job = new com.resumeopt.model.JobListing();
            job.setTitle("Sample Job");
            job.setDescription(jobDescription);
            
            // Perform skills gap analysis
            com.resumeopt.model.SkillsGapAnalysisResult result = 
                skillsGapAnalysisService.analyzeSkillsGap(resumeText, job);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("missingSkills", result.getMissingSkills());
            response.put("matchingSkills", result.getMatchingSkills());
            response.put("recommendations", result.getRecommendations());
            response.put("certificationSuggestions", result.getCertificationSuggestions());
            
            return response;
        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @Autowired
    private com.resumeopt.service.ChatGPTService chatGPTService;
    
    @PostMapping("/resume/fresher-career-advice")
    @ResponseBody
    public java.util.Map<String, Object> getFresherCareerAdvice(@RequestParam String resumeText, 
                                                  @RequestParam String jobDescription, 
                                                  @RequestParam(required = false) String academicBackground) {
        try {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            
            if (chatGPTService.isEnabled()) {
                String advice = chatGPTService.getFresherCareerAdvice(resumeText, jobDescription, academicBackground);
                response.put("advice", advice);
            } else {
                response.put("advice", "AI service is not configured. Please set up your API key in application.properties.");
            }
            
            response.put("success", true);
            return response;
        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    @PostMapping("/resume/fresher-resume-analysis")
    @ResponseBody
    public java.util.Map<String, Object> analyzeFresherResume(@RequestParam String resumeText, 
                                                  @RequestParam String jobDescription) {
        try {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            
            if (chatGPTService.isEnabled()) {
                String analysis = chatGPTService.analyzeFresherResumeForJob(resumeText, jobDescription);
                response.put("analysis", analysis);
            } else {
                response.put("analysis", "AI service is not configured. Please set up your API key in application.properties.");
            }
            
            response.put("success", true);
            return response;
        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @GetMapping("/resume")
    public String resumePage(Model model) {
        // Check if services are available
        boolean servicesAvailable = optimizationService != null && parserService != null;
        if (!servicesAvailable) {
            model.addAttribute("error", "Some services are not available. Please check server logs.");
        }
        return "resume";
    }
    
    @GetMapping("/resume-simple")
    public String resumeSimplePage() {
        return "resume-simple";
    }
    
    @GetMapping("/resume-test")
    public String resumeTestPage() {
        return "resume-test";
    }

    @PostMapping("/resume/optimize")
    public String optimize(@RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
                           @RequestParam(value = "resumeText", required = false) String resumeText,
                           @RequestParam(value = "jobDescription", required = false) String jobDescription,
                           @RequestParam(value = "design", required = false) String designName,
                           Model model) {
        try {
            // Validate inputs
            if (jobDescription == null || jobDescription.trim().isEmpty()) {
                model.addAttribute("error", "Please provide a job description.");
                return "resume";
            }
            
            String originalText;
            try {
                if (resumeFile != null && !resumeFile.isEmpty()) {
                    originalText = parserService.extractText(resumeFile);
                } else {
                    originalText = resumeText == null ? "" : resumeText.trim();
                }
            } catch (Exception e) {
                System.err.println("Error extracting text from file: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "Error reading resume file: " + e.getMessage());
                return "resume";
            }
            
            // Ensure we have some text to work with
            if (originalText == null || originalText.isEmpty()) {
                model.addAttribute("error", "Please provide resume text or upload a resume file.");
                return "resume";
            }
            
            System.out.println("DEBUG: Original text length: " + originalText.length());
            System.out.println("DEBUG: Job description length: " + jobDescription.length());

            // Optimize resume
            if (optimizationService == null) {
                System.err.println("ERROR: OptimizationService is null!");
                model.addAttribute("error", "Optimization service is not available. Please check server configuration.");
                return "resume";
            }
            
            com.resumeopt.service.ResumeOptimizationService.OptimizationResult result;
            try {
                System.out.println("DEBUG: Calling optimizationService.optimize()...");
                result = optimizationService.optimize(originalText, jobDescription);
                System.out.println("DEBUG: Optimization completed successfully");
            } catch (NullPointerException e) {
                System.err.println("ERROR: NullPointerException in optimization service: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "Internal error: Optimization service encountered a null reference. Please try again or contact support.");
                return "resume";
            } catch (Exception e) {
                System.err.println("ERROR: Exception in optimization service: " + e.getMessage());
                e.printStackTrace();
                String errorMsg = "Error optimizing resume: " + e.getMessage();
                if (e.getCause() != null) {
                    errorMsg += " (Cause: " + e.getCause().getMessage() + ")";
                }
                model.addAttribute("error", errorMsg);
                return "resume";
            }
            
            if (result == null) {
                System.err.println("ERROR: Optimization service returned null result");
                model.addAttribute("error", "Optimization service returned null result. Please try again.");
                return "resume";
            }
            System.out.println("DEBUG: Optimization result - Original score: " + result.originalScore() + ", Optimized score: " + result.optimizedScore());
            System.out.println("DEBUG: Injected keywords count: " + (result.injectedKeywords() != null ? result.injectedKeywords().size() : 0));
            
            // Determine design selection
            ResumeDesign selectedDesign = null;
            ResumeDesign recommendedDesign = null;
            
            try {
                if (designName != null && !designName.isBlank()) {
                    selectedDesign = ResumeDesign.fromString(designName);
                }
                
                // Get recommendation if no design selected
                if (selectedDesign == null && designService != null) {
                    try {
                        recommendedDesign = designService.recommendDesign(originalText, jobDescription);
                        selectedDesign = recommendedDesign; // Use recommended as default
                    } catch (Exception e) {
                        System.err.println("Error getting design recommendation: " + e.getMessage());
                        // Continue without design recommendation
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing design selection: " + e.getMessage());
                // Continue without design
            }
        
            Resume r = new Resume();
            r.setOriginalText(originalText);
            r.setOptimizedText(result.optimizedText() != null ? result.optimizedText() : originalText);
            r.setAtsOriginalScore(result.originalScore() * 100.0);
            r.setAtsOptimizedScore(result.optimizedScore() * 100.0);
            r.setSelectedDesign(selectedDesign);
            r.setRecommendedDesign(recommendedDesign);
            
            byte[] originalFileBytes = null;
            String contentType = null;
            if (resumeFile != null && !resumeFile.isEmpty()) {
                try {
                    r.setOriginalFilename(resumeFile.getOriginalFilename() != null ? resumeFile.getOriginalFilename() : "resume");
                    contentType = resumeFile.getContentType() != null ? resumeFile.getContentType() : "application/octet-stream";
                    r.setContentType(contentType);
                    // Store original file to preserve template
                    originalFileBytes = resumeFile.getBytes();
                    r.setOriginalFile(originalFileBytes);
                } catch (Exception e) {
                    System.err.println("Error reading file bytes: " + e.getMessage());
                    // Continue without original file bytes
                }
            }
        
            // Generate PDF preserving original template if available, or using selected design
            try {
                byte[] pdf;
                String optimizedText = result.optimizedText() != null ? result.optimizedText() : originalText;
                if (selectedDesign != null && pdfService != null) {
                    // Use selected design template
                    pdf = pdfService.generatePdf(optimizedText, originalFileBytes, originalText, selectedDesign);
                } else if (originalFileBytes != null && contentType != null && contentType.toLowerCase().contains("pdf") && pdfService != null) {
                    // Use original PDF as template with advanced preservation
                    pdf = pdfService.generatePdf(optimizedText, originalFileBytes, originalText);
                } else if (pdfService != null) {
                    // Generate new PDF
                    pdf = pdfService.generatePdf(optimizedText);
                } else {
                    pdf = null;
                    System.err.println("PDF service not available");
                }
                if (pdf != null) {
                    r.setOptimizedPdf(pdf);
                }
            } catch (Exception e) {
                // keep going without PDF
                System.err.println("Error generating PDF: " + e.getMessage());
                e.printStackTrace();
            }

            // If uploaded DOCX, preserve template and replace content
            try {
                if (originalFileBytes != null && contentType != null && 
                    contentType.toLowerCase().contains("officedocument.wordprocessingml.document") && docService != null){
                    var docRes = docService.replaceContentWithOptimized(originalFileBytes, originalText,
                            result.optimizedText(), result.injectedKeywords() != null ? result.injectedKeywords() : java.util.Collections.emptyList());
                    r.setOptimizedDocx(docRes.docxBytes);
                    r.setChangeLogText(docRes.changeLogText);
                } else {
                    r.setChangeLogText("Resume optimized; see insights and injected keywords.");
                }
            } catch (Exception e) {
                System.err.println("Error processing DOCX: " + e.getMessage());
                e.printStackTrace();
                r.setChangeLogText("Resume optimized; see insights and injected keywords.");
            }
            
            // Save resume to database
            Resume savedResume;
            try {
                savedResume = resumeRepository.save(r);
                System.out.println("DEBUG: Resume saved with ID: " + savedResume.getId());
            } catch (Exception e) {
                System.err.println("Error saving resume to database: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "Error saving resume: " + e.getMessage());
                return "resume";
            }
        
            // Generate and store changes
            try {
                if (diffService != null && changeRepository != null) {
                    java.util.List<ResumeChange> changes = diffService.generateChanges(
                        originalText, result.optimizedText() != null ? result.optimizedText() : originalText, savedResume);
                    
                    // Only save meaningful changes
                    int savedCount = 0;
                    for (ResumeChange change : changes) {
                        try {
                            // Validate change has required fields
                            if (change != null && change.getChangeType() != null && 
                                (change.getOriginalText() != null || change.getNewText() != null)) {
                                changeRepository.save(change);
                                savedCount++;
                            }
                        } catch (Exception e) {
                            System.err.println("Error saving individual change: " + e.getMessage());
                            // Continue with next change
                        }
                    }
                    System.out.println("DEBUG: Generated " + changes.size() + " changes, saved " + savedCount + " valid changes");
                }
            } catch (Exception e) {
                System.err.println("Error generating changes: " + e.getMessage());
                e.printStackTrace();
                // Continue - changes are optional
            }

            // Set model attributes
            model.addAttribute("originalText", originalText);
            model.addAttribute("optimizedText", result.optimizedText() != null ? result.optimizedText() : originalText);
            model.addAttribute("atsOriginal", String.format("%.0f", result.originalScore() * 100));
            model.addAttribute("atsOptimized", String.format("%.0f", result.optimizedScore() * 100));
            model.addAttribute("improvement", String.format("%.0f", (result.optimizedScore() - result.originalScore()) * 100));
            model.addAttribute("atsOriginalNum", result.originalScore() * 100);
            model.addAttribute("atsOptimizedNum", result.optimizedScore() * 100);
            // Use strict optimized ATS score for internal readiness as mapping helper was removed
            model.addAttribute("atsOptimizedReadinessInternal", String.format("%.0f", result.optimizedScore() * 100));
            model.addAttribute("injectedKeywords", result.injectedKeywords() != null ? result.injectedKeywords() : java.util.Collections.emptyList());
            model.addAttribute("insights", result.insights() != null ? result.insights() : java.util.Collections.emptyList());
            model.addAttribute("resumeId", savedResume.getId());
            model.addAttribute("changeLogText", savedResume.getChangeLogText() != null ? savedResume.getChangeLogText() : "Resume optimized.");
            model.addAttribute("selectedDesign", selectedDesign != null ? selectedDesign.getDisplayName() : "Default");
            model.addAttribute("recommendedDesign", recommendedDesign != null ? recommendedDesign.getDisplayName() : null);
            
            // Add design preview if available
            try {
                if (selectedDesign != null && designService != null) {
                    model.addAttribute("designPreview", designService.getDesignPreview(selectedDesign));
                }
            } catch (Exception e) {
                System.err.println("Error getting design preview: " + e.getMessage());
                // Continue without preview
            }
            
            System.out.println("DEBUG: Model attributes set - resumeId: " + savedResume.getId() + ", originalText length: " + originalText.length());
            System.out.println("DEBUG: Insights count: " + (result.insights() != null ? result.insights().size() : 0) + ", Keywords count: " + (result.injectedKeywords() != null ? result.injectedKeywords().size() : 0));
            System.out.println("DEBUG: All model attributes: " + model.asMap().keySet());

            // Publish event
            try {
                if (events != null) {
                    events.publish("/topic/resume/optimized", new java.util.HashMap<String, Object>() {{
                        put("atsOriginal", (int)Math.round(result.originalScore() * 100));
                        put("atsOptimized", (int)Math.round(result.optimizedScore() * 100));
                        put("optimizedText", result.optimizedText() != null ? result.optimizedText() : originalText);
                        put("injectedKeywords", result.injectedKeywords() != null ? result.injectedKeywords() : java.util.Collections.emptyList());
                        put("insights", result.insights() != null ? result.insights() : java.util.Collections.emptyList());
                        put("pdfUrl", "/resume/pdf/" + savedResume.getId());
                        put("docxUrl", "/resume/docx/" + savedResume.getId());
                        put("ts", System.currentTimeMillis());
                    }});
                }
            } catch (Exception e) {
                System.err.println("Error publishing event: " + e.getMessage());
                // Continue - event publishing is optional
            }
            
            return "resume";
            
        } catch (Exception e) {
            System.err.println("Unexpected error in optimize method: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "resume";
        }
    }

    @GetMapping("/resume/pdf/{id}")
    public org.springframework.http.ResponseEntity<byte[]> download(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        if (id == null) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
        java.util.Optional<Resume> opt = resumeRepository.findById(id);
        if (opt.isEmpty() || opt.get().getOptimizedPdf() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        Resume resume = opt.get();
        byte[] pdfData = resume.getOptimizedPdf();
        if (pdfData == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=optimized_resume.pdf")
                .contentType(java.util.Objects.requireNonNull(org.springframework.http.MediaType.APPLICATION_PDF))
                .body(pdfData);
    }

    @GetMapping("/resume/docx/{id}")
    public org.springframework.http.ResponseEntity<byte[]> downloadDocx(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        if (id == null) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
        java.util.Optional<Resume> opt = resumeRepository.findById(id);
        if (opt.isEmpty() || opt.get().getOptimizedDocx() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        Resume resume = opt.get();
        byte[] docxData = resume.getOptimizedDocx();
        if (docxData == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        org.springframework.http.MediaType docxMediaType = java.util.Objects.requireNonNull(
                org.springframework.http.MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=optimized_resume.docx")
                .contentType(docxMediaType)
                .body(docxData);
    }

    @GetMapping("/resume/original/{id}")
    public org.springframework.http.ResponseEntity<byte[]> downloadOriginal(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        if (id == null) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
        java.util.Optional<Resume> opt = resumeRepository.findById(id);
        if (opt.isEmpty() || opt.get().getOriginalFile() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        Resume resume = opt.get();
        byte[] original = resume.getOriginalFile();
        if (original == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        String filename = resume.getOriginalFilename() != null ? resume.getOriginalFilename() : "original_resume";
        String contentType = resume.getContentType() != null ? resume.getContentType() : "application/octet-stream";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(java.util.Objects.requireNonNull(org.springframework.http.MediaType.parseMediaType(contentType)))
                .body(original);
    }

    @GetMapping("/resume-debug")
    public String resumeDebugPage() {
        return "resume-debug";
    }

    @PostMapping("/resume/optimize-debug")
    public String optimizeDebug(@RequestParam("resumeText") String resumeText,
                               @RequestParam("jobDescription") String jobDescription,
                               Model model) throws IOException {
        
        System.out.println("DEBUG ENDPOINT: Resume text length: " + resumeText.length());
        System.out.println("DEBUG ENDPOINT: Job description length: " + jobDescription.length());

        var result = optimizationService.optimize(resumeText, jobDescription);
        
        Resume r = new Resume();
        r.setOriginalText(resumeText);
        r.setOptimizedText(result.optimizedText());
        r.setAtsOriginalScore(result.originalScore() * 100.0);
        r.setAtsOptimizedScore(result.optimizedScore() * 100.0);
        
        Resume savedResume = resumeRepository.save(r);
        System.out.println("DEBUG ENDPOINT: Resume saved with ID: " + savedResume.getId());

        model.addAttribute("originalText", resumeText);
        model.addAttribute("optimizedText", result.optimizedText());
        model.addAttribute("atsOriginal", String.format("%.0f", result.originalScore() * 100));
        model.addAttribute("atsOptimized", String.format("%.0f", result.optimizedScore() * 100));
        model.addAttribute("injectedKeywords", result.injectedKeywords());
        model.addAttribute("insights", result.insights());
        model.addAttribute("resumeId", savedResume.getId());
        
        System.out.println("DEBUG ENDPOINT: Returning to resume-debug template");
        return "resume-debug";
    }

    @GetMapping("/resume/template/{id}")
    public String previewTemplate(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                                  @RequestParam(name = "style", required = false, defaultValue = "MINIMAL") String styleParam,
                                  Model model) {
        if (id == null) {
            return "redirect:/resume";
        }
        java.util.Optional<Resume> opt = resumeRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/resume";
        }
        Resume resume = opt.get();
        String text = resume.getOptimizedText() != null && !resume.getOptimizedText().isBlank()
                ? resume.getOptimizedText()
                : resume.getOriginalText();
        if (text == null) text = "";

        StructuredResumeView view = structuringService.buildView(text);
        ResumeTemplateStyle style = ResumeTemplateStyle.fromString(styleParam);

        model.addAttribute("structuredResume", view);
        model.addAttribute("templateStyle", style.name().toLowerCase());

        return switch (style) {
            case PROFESSIONAL -> "resume_template_professional";
            case MODERN -> "resume_template_modern";
            case TECH -> "resume_template_tech";
            case MINIMAL -> "resume_template_minimal";
            case FRESHER -> "resume_template_fresher";
            default -> "resume_template_minimal";
        };
    }

    @GetMapping("/resume/pdf/{id}/template")
    public org.springframework.http.ResponseEntity<byte[]> downloadTemplatePdf(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id,
            @RequestParam(name = "style", required = false, defaultValue = "MINIMAL") String styleParam) {
        if (id == null) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
        java.util.Optional<Resume> opt = resumeRepository.findById(id);
        if (opt.isEmpty()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        Resume resume = opt.get();
        String text = resume.getOptimizedText() != null && !resume.getOptimizedText().isBlank()
                ? resume.getOptimizedText()
                : resume.getOriginalText();
        if (text == null) text = "";

        StructuredResumeView view = structuringService.buildView(text);
        ResumeTemplateStyle style = ResumeTemplateStyle.fromString(styleParam);

        try {
            byte[] pdf = pdfService.generateTemplatePdf(view, style);
            String fileName = "resume_template_" + style.name().toLowerCase() + ".pdf";
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/resume/diff/{id}")
    public String visualDiff(@org.springframework.web.bind.annotation.PathVariable("id") Long id, Model model){
        if (id == null) {
            return "redirect:/resume";
        }
        Resume r = resumeRepository.findById(id).orElseThrow();
        model.addAttribute("originalText", r.getOriginalText());
        model.addAttribute("optimizedText", r.getOptimizedText());
        model.addAttribute("changeLogText", r.getChangeLogText());
        model.addAttribute("resumeId", id);
        return "resume_diff";
    }
    
    @GetMapping("/resume/changes/{id}")
    public org.springframework.http.ResponseEntity<java.util.List<ResumeChange>> getChanges(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        try {
            java.util.List<ResumeChange> changes = changeService.getChanges(id);
            return org.springframework.http.ResponseEntity.ok(changes);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/resume/changes/{changeId}/accept")
    public org.springframework.http.ResponseEntity<ResumeChange> acceptChange(
            @org.springframework.web.bind.annotation.PathVariable("changeId") Long changeId) {
        try {
            ResumeChange change = changeService.acceptChange(changeId);
            return org.springframework.http.ResponseEntity.ok(change);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/resume/changes/{changeId}/decline")
    public org.springframework.http.ResponseEntity<ResumeChange> declineChange(
            @org.springframework.web.bind.annotation.PathVariable("changeId") Long changeId) {
        try {
            ResumeChange change = changeService.declineChange(changeId);
            return org.springframework.http.ResponseEntity.ok(change);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/resume/preview/{id}")
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> getPreview(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        try {
            String preview = changeService.generatePreview(id);
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("preview", preview);
            response.put("statistics", changeService.getChangeStatistics(id));
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/resume/finalize/{id}")
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> finalizeResume(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        try {
            Resume resume = changeService.finalizeResume(id);
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("resumeId", resume.getId());
            response.put("finalText", resume.getFinalApprovedText());
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return org.springframework.http.ResponseEntity.status(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}