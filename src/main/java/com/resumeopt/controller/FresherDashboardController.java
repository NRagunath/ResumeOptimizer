package com.resumeopt.controller;

import com.resumeopt.model.FresherApplication;
import com.resumeopt.service.FresherApplicationTrackingService;
import com.resumeopt.service.JobSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/fresher")
public class FresherDashboardController {
    
    @Autowired
    private FresherApplicationTrackingService applicationTrackingService;
    
    @Autowired
    private JobSourceService jobSourceService;
    
    @GetMapping("/dashboard")
    public String fresherDashboard(Model model) {
        // Get fresher-specific application statistics
        List<FresherApplication> allApplications = applicationTrackingService.getAllApplications();
        List<FresherApplication> appliedApplications = applicationTrackingService.getApplicationsByStatus("Applied");
        List<FresherApplication> interviewApplications = applicationTrackingService.getApplicationsByStatus("Interview");
        List<FresherApplication> acceptedApplications = applicationTrackingService.getApplicationsByStatus("Accepted");
        List<FresherApplication> campusApplications = applicationTrackingService.getCampusRecruitingApplications();
        List<FresherApplication> internshipToFulltime = applicationTrackingService.getInternshipToFulltimeApplications();
        
        // Get fresher-friendly jobs
        // This would use the enhanced job filtering we added earlier
        // For now, we'll use all jobs but in a real implementation, this would filter for entry-level
        
        model.addAttribute("totalApplications", allApplications.size());
        model.addAttribute("appliedApplications", appliedApplications.size());
        model.addAttribute("interviewApplications", interviewApplications.size());
        model.addAttribute("acceptedApplications", acceptedApplications.size());
        model.addAttribute("campusApplications", campusApplications.size());
        model.addAttribute("internshipToFulltimeApplications", internshipToFulltime.size());
        model.addAttribute("applications", allApplications);
        
        return "fresher/dashboard";
    }
    
    @GetMapping("/applications")
    public String fresherApplications(Model model) {
        List<FresherApplication> applications = applicationTrackingService.getAllApplications();
        model.addAttribute("applications", applications);
        return "fresher/applications";
    }
    
    @GetMapping("/recommendations")
    public String fresherJobRecommendations(Model model) {
        // Get entry-level job recommendations
        // This would use the enhanced job recommendation system we created earlier
        return "fresher/recommendations";
    }
}