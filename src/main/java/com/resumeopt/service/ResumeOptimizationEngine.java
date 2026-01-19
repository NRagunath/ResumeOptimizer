package com.resumeopt.service;

import com.resumeopt.model.ResumeTemplateStyle;
import com.resumeopt.model.StructuredResumeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Main integration service that combines AI-powered optimization,
 * ATS-safe formatting, and PDF generation into a complete resume
 * optimization pipeline.
 * 
 * This service provides a single entry point for:
 * 1. Optimizing structured resume data against a job description
 * 2. Formatting the optimized resume as ATS-safe text
 * 3. Generating browser-rendered PDF output
 */
@Service
public class ResumeOptimizationEngine {

    @Autowired
    private AIPoweredResumeEngine aiEngine;

    @Autowired
    private ATSResumeFormatter formatter;

    @Autowired
    private BrowserPdfGenerator pdfGenerator;

    @Autowired
    private ResumeStructuringService structuringService;

    /**
     * Complete optimization result containing all outputs
     */
    public record CompleteOptimizationResult(
            StructuredResumeView optimizedResume,
            String formattedText,
            byte[] pdfBytes,
            double atsScore,
            java.util.List<String> matchedKeywords,
            java.util.List<String> optimizationNotes,
            java.util.Map<String, Integer> keywordUsageCounts
    ) {}

    /**
     * Optimizes a resume from structured data and job description.
     * Returns complete result with formatted text and PDF.
     */
    public CompleteOptimizationResult optimize(
            StructuredResumeView resumeData,
            String jobDescription,
            ResumeTemplateStyle templateStyle) throws IOException {

        // Step 1: AI-powered optimization
        AIPoweredResumeEngine.OptimizationResult optimizationResult = 
                aiEngine.optimize(resumeData, jobDescription);

        // Step 2: Format as ATS-safe text
        String formattedText = formatter.formatAsText(
                optimizationResult.optimizedResume(), 
                templateStyle != null ? templateStyle : ResumeTemplateStyle.MINIMAL);

        // Step 3: Generate PDF
        byte[] pdfBytes = pdfGenerator.generatePdf(
                optimizationResult.optimizedResume(),
                templateStyle != null ? templateStyle : ResumeTemplateStyle.MINIMAL);

        return new CompleteOptimizationResult(
                optimizationResult.optimizedResume(),
                formattedText,
                pdfBytes,
                optimizationResult.atsScore(),
                optimizationResult.matchedKeywords(),
                optimizationResult.optimizationNotes(),
                optimizationResult.keywordUsageCounts()
        );
    }

    /**
     * Optimizes a resume from plain text and job description.
     * First parses text into structured format, then optimizes.
     */
    public CompleteOptimizationResult optimizeFromText(
            String resumeText,
            String jobDescription,
            ResumeTemplateStyle templateStyle) throws IOException {

        // Parse text into structured format
        StructuredResumeView structuredResume = structuringService.buildView(resumeText);

        // Optimize
        return optimize(structuredResume, jobDescription, templateStyle);
    }

    /**
     * Quick optimization that returns only the optimized structured resume
     */
    public StructuredResumeView optimizeResume(
            StructuredResumeView resumeData,
            String jobDescription) {

        AIPoweredResumeEngine.OptimizationResult result = 
                aiEngine.optimize(resumeData, jobDescription);
        
        return result.optimizedResume();
    }
}

