package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Filters job listings to keep only true fresher / 0–1 year roles.
 * Uses both explicit "years of experience" patterns and title/description keywords.
 */
@Service
public class JobExperienceFilterService {

    public List<JobListing> filterFreshers(List<JobListing> jobs) {
        if (jobs == null) return List.of();
        return jobs.stream()
                .filter(this::isFresherJob)
                .collect(Collectors.toList());
    }

    private boolean isFresherJob(JobListing job) {
        String title = (job.getTitle() == null ? "" : job.getTitle()).toLowerCase(Locale.ROOT);
        String desc = (job.getDescription() == null ? "" : job.getDescription()).toLowerCase(Locale.ROOT);
        String text = title + " " + desc;

        // 1) Hard block any obviously senior titles, regardless of description.
        if (title.matches(".*\\b(senior|sr\\.|principal|lead|manager|architect|staff)\\b.*")) {
            return false;
        }

        // 2) Only keep roles that clearly look like fresher / 0–1 year jobs.
        //    We DO NOT try to interpret arbitrary ranges like "4 to 6 yrs", "3–5 years" etc.
        //    Instead we require explicit fresher-style keywords or 0–1 year phrases.
        boolean fresherKeywords = text.matches(".*(fresher|freshers|graduate|entry level|entry-level|junior|trainee|intern).*");
        boolean zeroOnePatterns = text.matches(".*(0-1\\s*years?|0\\s*to\\s*1\\s*years?|0\\s*yrs?|1\\s*year).*");
        boolean seniorSignals = text.matches(".*(senior|lead|manager|architect|principal|staff).*");

        if (seniorSignals) {
            return false;
        }

        // Keep only if it positively looks like an entry-level role.
        return fresherKeywords || zeroOnePatterns;
    }
}


