package com.resumeopt.controller;

import com.resumeopt.model.ApplicationStatus;
import com.resumeopt.model.JobApplication;
import com.resumeopt.repo.JobApplicationRepository;
import com.resumeopt.service.ApplicationTrackingService;
import com.resumeopt.realtime.RealtimeEventPublisher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@Controller
public class TrackingController {
    private final JobApplicationRepository repo;
    private final ApplicationTrackingService trackingService;
    private final RealtimeEventPublisher events;

    public TrackingController(JobApplicationRepository repo, ApplicationTrackingService trackingService, RealtimeEventPublisher events) {
        this.repo = repo;
        this.trackingService = trackingService;
        this.events = events;
    }

    @GetMapping("/tracking")
    public String dashboard(Model model) {
        Map<ApplicationStatus, Long> counts = trackingService.statusCounts();
        model.addAttribute("counts", counts);
        model.addAttribute("applications", repo.findAll());
        return "tracking";
    }

    @PostMapping("/tracking/add")
    public String add(
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("companyName") String companyName,
            @RequestParam("applyUrl") String applyUrl,
            @RequestParam("status") ApplicationStatus status,
            @RequestParam("notes") String notes,
            @RequestParam("applicationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applicationDate
    ) {
        JobApplication app = new JobApplication();
        app.setJobTitle(jobTitle);
        app.setCompanyName(companyName);
        app.setApplyUrl(applyUrl);
        app.setStatus(status);
        app.setNotes(notes);
        app.setApplicationDate(applicationDate);
        app.setInterviewLikelihood(trackingService.estimateInterviewLikelihood(app));
        repo.save(app);
        events.publish("/topic/tracking/applications", app);
        return "redirect:/tracking";
    }

    @GetMapping("/tracking/export")
    public ResponseEntity<byte[]> export() throws IOException {
        byte[] bytes = trackingService.exportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applications.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // Lightweight tracking endpoint invoked on Apply clicks
    @PostMapping("/jobs/track")
    public ResponseEntity<Void> trackApply(@RequestBody Map<String, String> payload) {
        JobApplication app = new JobApplication();
        app.setJobTitle(payload.getOrDefault("jobTitle", ""));
        app.setCompanyName(payload.getOrDefault("companyName", ""));
        app.setApplyUrl(payload.getOrDefault("applyUrl", ""));
        app.setStatus(ApplicationStatus.APPLIED);
        app.setApplicationDate(LocalDate.now());
        app.setInterviewLikelihood(trackingService.estimateInterviewLikelihood(app));
        repo.save(app);
        events.publish("/topic/tracking/applications", app);
        return ResponseEntity.noContent().build();
    }
}