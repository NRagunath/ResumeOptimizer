package com.resumeopt.service;

import com.resumeopt.model.ApplicationStatus;
import com.resumeopt.model.JobApplication;
import com.resumeopt.repo.JobApplicationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApplicationTrackingService {
    private final JobApplicationRepository repo;

    public ApplicationTrackingService(JobApplicationRepository repo) {
        this.repo = repo;
    }

    @io.micrometer.core.annotation.Timed(value = "tracking.statusCounts", description = "Compute status counts")
    public Map<ApplicationStatus, Long> statusCounts() {
        return repo.findAll().stream().collect(Collectors.groupingBy(JobApplication::getStatus, Collectors.counting()));
    }

    public double estimateInterviewLikelihood(JobApplication app) {
        double base = 0.2; // base chance
        if (app.getStatus() == ApplicationStatus.INTERVIEW) base = 0.7;
        if (app.getStatus() == ApplicationStatus.OFFER) base = 0.95;
        if (app.getApplicationDate() != null && app.getApplicationDate().isAfter(LocalDate.now().minusDays(7))) base += 0.1;
        return Math.min(1.0, base);
    }

    @io.micrometer.core.annotation.Timed(value = "tracking.export", description = "Export Excel time")
    public byte[] exportExcel() throws IOException {
        List<JobApplication> apps = repo.findAll();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Applications");
            Row header = sheet.createRow(0);
            String[] cols = {"Job Title", "Company Name", "Application Date", "Status", "Notes"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
            }
            int rowIdx = 1;
            for (JobApplication app : apps) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(s(app.getJobTitle()));
                r.createCell(1).setCellValue(s(app.getCompanyName()));
                r.createCell(2).setCellValue(app.getApplicationDate() == null ? "" : app.getApplicationDate().toString());
                r.createCell(3).setCellValue(app.getStatus().name());
                r.createCell(4).setCellValue(s(app.getNotes()));
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private String s(String v) { return v == null ? "" : v; }
}