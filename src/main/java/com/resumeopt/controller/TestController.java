package com.resumeopt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.resumeopt.service.ResumeOptimizationService;

@Controller
public class TestController {

    @Autowired
    private ResumeOptimizationService optimizationService;

    @GetMapping("/test")
    public String testPage() {
        return "test";
    }

    @PostMapping("/test/submit")
    public String testSubmit(@RequestParam("testText") String testText, Model model) {
        System.out.println("DEBUG: Test text received: " + testText);
        
        model.addAttribute("receivedText", testText);
        model.addAttribute("textLength", testText.length());
        model.addAttribute("success", true);
        
        System.out.println("DEBUG: Model attributes set successfully");
        
        return "test";
    }

    @GetMapping("/test/resume-optimization")
    public String testResumeOptimization(Model model) {
        try {
            System.out.println("DEBUG: Testing resume optimization service...");
            
            String testResume = "John Doe\nSoftware Developer\nExperience: 2 years in Java development\nSkills: Java, Spring Boot, MySQL";
            String testJobDescription = "We are looking for a Java Developer with Spring Boot experience and database knowledge. Requirements: Java, Spring Boot, REST API, MySQL, Git";
            
            System.out.println("DEBUG: Calling optimization service...");
            ResumeOptimizationService.OptimizationResult result = optimizationService.optimize(testResume, testJobDescription);
            
            System.out.println("DEBUG: Optimization completed successfully");
            System.out.println("DEBUG: Original score: " + result.originalScore());
            System.out.println("DEBUG: Optimized score: " + result.optimizedScore());
            
            model.addAttribute("testSuccess", true);
            model.addAttribute("originalText", testResume);
            model.addAttribute("optimizedText", result.optimizedText());
            model.addAttribute("originalScore", String.format("%.0f", result.originalScore() * 100));
            model.addAttribute("optimizedScore", String.format("%.0f", result.optimizedScore() * 100));
            model.addAttribute("keywords", result.injectedKeywords());
            model.addAttribute("insights", result.insights());
            
        } catch (Exception e) {
            System.err.println("ERROR: Resume optimization test failed: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("testError", "Resume optimization failed: " + e.getMessage());
        }
        
        return "test";
    }
}