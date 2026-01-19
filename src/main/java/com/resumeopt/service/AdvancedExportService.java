package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Advanced export service for multiple formats
 */
@Service
public class AdvancedExportService {
    
    /**
     * Export jobs to CSV format
     */
    public byte[] exportToCSV(List<JobListing> jobs) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        
        // Write header
        writer.println("Title,Company,Location,Salary,Job Type,Source,Posted Date,Deadline,Apply URL,Description");
        
        // Write data
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (JobListing job : jobs) {
            writer.print(escapeCSV(job.getTitle()));
            writer.print(",");
            writer.print(escapeCSV(job.getCompany()));
            writer.print(",");
            writer.print(escapeCSV(job.getLocation()));
            writer.print(",");
            writer.print(escapeCSV(job.getSalaryRange()));
            writer.print(",");
            writer.print(job.getJobType() != null ? job.getJobType().name() : "");
            writer.print(",");
            writer.print(job.getSource() != null ? job.getSource().name() : "");
            writer.print(",");
            writer.print(job.getPostedDate() != null ? job.getPostedDate().format(formatter) : "");
            writer.print(",");
            writer.print(job.getApplicationDeadline() != null ? job.getApplicationDeadline().format(formatter) : "");
            writer.print(",");
            writer.print(escapeCSV(job.getApplyUrl()));
            writer.print(",");
            writer.print(escapeCSV(job.getDescription()));
            writer.println();
        }
        
        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }
    
    /**
     * Export jobs to JSON format
     */
    public String exportToJSON(List<JobListing> jobs) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"jobs\": [\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        for (int i = 0; i < jobs.size(); i++) {
            JobListing job = jobs.get(i);
            json.append("    {\n");
            json.append("      \"title\": \"").append(escapeJSON(job.getTitle())).append("\",\n");
            json.append("      \"company\": \"").append(escapeJSON(job.getCompany())).append("\",\n");
            json.append("      \"location\": \"").append(escapeJSON(job.getLocation())).append("\",\n");
            json.append("      \"salaryRange\": \"").append(escapeJSON(job.getSalaryRange())).append("\",\n");
            json.append("      \"jobType\": \"").append(job.getJobType() != null ? job.getJobType().name() : "").append("\",\n");
            json.append("      \"source\": \"").append(job.getSource() != null ? job.getSource().name() : "").append("\",\n");
            json.append("      \"postedDate\": \"").append(job.getPostedDate() != null ? job.getPostedDate().format(formatter) : "").append("\",\n");
            json.append("      \"deadline\": \"").append(job.getApplicationDeadline() != null ? job.getApplicationDeadline().format(formatter) : "").append("\",\n");
            json.append("      \"applyUrl\": \"").append(escapeJSON(job.getApplyUrl())).append("\",\n");
            json.append("      \"description\": \"").append(escapeJSON(job.getDescription())).append("\"\n");
            json.append("    }");
            if (i < jobs.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n}");
        return json.toString();
    }
    
    /**
     * Escape CSV field
     */
    private String escapeCSV(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJSON(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

