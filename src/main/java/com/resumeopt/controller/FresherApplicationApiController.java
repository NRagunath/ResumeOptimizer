package com.resumeopt.controller;

import com.resumeopt.model.FresherApplication;
import com.resumeopt.service.FresherApplicationTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fresher")
public class FresherApplicationApiController {
    
    @Autowired
    private FresherApplicationTrackingService applicationTrackingService;
    
    @PostMapping("/applications")
    public ResponseEntity<Map<String, Object>> createApplication(@RequestBody FresherApplication application) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Set default values if not provided
            if (application.getApplicationDate() == null) {
                application.setApplicationDate(LocalDateTime.now());
            }
            
            FresherApplication savedApplication = applicationTrackingService.saveApplication(application);
            
            response.put("success", true);
            response.put("message", "Application saved successfully");
            response.put("application", savedApplication);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PutMapping("/applications/{id}/status")
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<FresherApplication> applicationOpt = applicationTrackingService.getApplicationById(id);
            
            if (applicationOpt.isPresent()) {
                FresherApplication application = applicationOpt.get();
                application.setApplicationStatus(request.get("status"));
                application.setUpdatedAt(LocalDateTime.now());
                
                FresherApplication updatedApplication = applicationTrackingService.saveApplication(application);
                
                response.put("success", true);
                response.put("message", "Application status updated successfully");
                response.put("application", updatedApplication);
                
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Application not found");
                
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Map<String, Object>> deleteApplication(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            applicationTrackingService.deleteApplication(id);
            
            response.put("success", true);
            response.put("message", "Application deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/applications")
    public ResponseEntity<Map<String, Object>> getAllApplications() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("success", true);
            response.put("applications", applicationTrackingService.getAllApplications());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}