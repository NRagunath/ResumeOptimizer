package com.resumeopt.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class ResumeOptimizationServiceTest {

    private final ResumeOptimizationService optimizationService = new ResumeOptimizationService();

    @Test
    void testOptimize_WithHighQualityResume_ShouldReturnHighScore() {
        String jobDescription = "We are looking for a Senior Java Developer with experience in Spring Boot, Microservices, and AWS. " +
                "Must have strong communication skills and leadership ability. Experience with CI/CD and Docker is a plus.";

        String resumeText = """
                John Doe
                johndoe@example.com | 123-456-7890
                
                Professional Summary
                Experienced Senior Java Developer with 8 years of experience in building scalable applications.
                
                Skills
                Java, Spring Boot, Microservices, AWS, Docker, Kubernetes, CI/CD, SQL, Git.
                Communication, Leadership, Teamwork.
                
                Experience
                Senior Developer | Tech Corp | 2018 - Present
                - Led a team of 5 developers to build a microservices architecture.
                - Improved application performance by 30% through optimization.
                - Reduced deployment time by 50% using CI/CD pipelines.
                - Managed a budget of $50k for cloud infrastructure.
                - Delivered 10+ major features on time.
                
                Education
                Bachelor of Science in Computer Science | University of Tech
                """;

        var result = optimizationService.optimize(resumeText, jobDescription);

        System.out.println("Original Score: " + result.originalScore());
        System.out.println("Optimized Score: " + result.optimizedScore());
        System.out.println("Missing Keywords: " + result.injectedKeywords());

        // Expect high score because of measurable achievements (numbers), standard headers, and keyword matches
        assertTrue(result.originalScore() > 0.85, "Score should be above 0.85 for a high quality resume");
    }

    @Test
    void testOptimize_WithPoorResume_ShouldReturnLowScore() {
        String jobDescription = "Looking for a Java Developer.";
        
        String resumeText = """
                John Doe
                
                Worked as a developer.
                Knows Java.
                """;

        var result = optimizationService.optimize(resumeText, jobDescription);
        
        System.out.println("Poor Resume Score: " + result.originalScore());
        
        assertTrue(result.originalScore() < 0.5, "Score should be low for a poor resume");
    }
    
    @Test
    void testMeasurableAchievements_Impact() {
        String jobDescription = "Java Developer role.";
        
        String resumeNoMetrics = """
                Jane Doe
                jane@example.com | 987-654-3210
                
                Experience
                Developer
                - Worked on Java projects.
                - Fixed bugs.
                - Wrote code.
                """;
                
        String resumeWithMetrics = """
                Jane Doe
                jane@example.com | 987-654-3210
                
                Experience
                Developer
                - Worked on 5+ Java projects.
                - Fixed 100+ bugs, improving stability by 20%.
                - Wrote code for 3 modules.
                """;
                
        var resultNoMetrics = optimizationService.optimize(resumeNoMetrics, jobDescription);
        var resultWithMetrics = optimizationService.optimize(resumeWithMetrics, jobDescription);
        
        System.out.println("Score without metrics: " + resultNoMetrics.originalScore());
        System.out.println("Score with metrics: " + resultWithMetrics.originalScore());
        
        assertTrue(resultWithMetrics.originalScore() > resultNoMetrics.originalScore(), "Resume with metrics should score higher");
    }
}
