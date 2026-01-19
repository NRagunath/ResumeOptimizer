package com.resumeopt.service;

import com.resumeopt.model.JobListing;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class JobFilterService {

    // Strict role-based filtering: allow only IT/software engineering/developer roles
    public boolean isRelevant(JobListing jl){
        String title = (jl.getTitle()==null?"":jl.getTitle()).toLowerCase(Locale.ROOT);
        String desc = (jl.getDescription()==null?"":jl.getDescription()).toLowerCase(Locale.ROOT);

        boolean allowedTitle = title.matches(".*(software|developer|engineer|backend|frontend|full\s*stack|qa|test|devops|data\s*engineer).*");
        boolean allowedDesc = desc.matches(".*(java|python|javascript|react|angular|spring|sql|api|docker|kubernetes|git|testing|ci/cd|unit).*");

        boolean excluded = title.matches(".*(sales|support|hr|recruiter|marketing|content|non\s*tech).*");

        return (allowedTitle || allowedDesc) && !excluded;
    }

    public List<JobListing> filterRelevant(List<JobListing> list){
        return list.stream().filter(this::isRelevant).collect(Collectors.toList());
    }
}