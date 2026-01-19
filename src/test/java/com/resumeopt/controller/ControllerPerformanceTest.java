package com.resumeopt.controller;

import com.resumeopt.model.JobListing;
import com.resumeopt.service.JobSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ControllerPerformanceTest {

    @Mock
    private JobSourceService jobSourceService;

    @Mock
    private Model model;

    @InjectMocks
    private JobsController jobsController;

    @Test
    public void verifySingleServiceCall() {
        // Arrange
        List<JobListing> mockJobs = new ArrayList<>();
        when(jobSourceService.aggregateAllListings()).thenReturn(mockJobs);

        // Act
        jobsController.showJobs(model, 1, 30);

        // Assert
        // Verify that aggregateAllListings was called EXACTLY ONCE
        verify(jobSourceService, times(1)).aggregateAllListings();
        
        // Also verify that the model was populated with freshJobs and weeklyJobs
        verify(model).addAttribute(eq("freshJobs"), anyList());
        verify(model).addAttribute(eq("weeklyJobs"), anyList());
    }
}
