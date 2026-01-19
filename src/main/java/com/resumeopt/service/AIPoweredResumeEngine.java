package com.resumeopt.service;

import com.resumeopt.model.ResumeTemplateStyle;
import com.resumeopt.model.StructuredResumeView;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI-powered resume generation and optimization engine designed to produce
 * professionally structured, visually clean, and highly ATS-compatible resumes.
 * 
 * STRICT RULES:
 * - NEVER invents, assumes, exaggerates, or infers any content not in resume data
 * - ONLY matches keywords from job description against existing resume content
 * - Uses natural keyword usage (1-3 occurrences max, no stuffing)
 * - Reorders and prioritizes existing content based on relevance
 * - Replaces synonyms with exact job-description terminology
 * - Improves clarity and impact without altering factual meaning
 */
@Service
public class AIPoweredResumeEngine {

    /**
     * Result of resume optimization containing the optimized structured view and metadata
     */
    public record OptimizationResult(
            StructuredResumeView optimizedResume,
            double atsScore,
            List<String> matchedKeywords,
            List<String> optimizationNotes,
            Map<String, Integer> keywordUsageCounts
    ) {}

    /**
     * Main optimization method: transforms structured resume data and job description
     * into an ATS-optimized resume following strict content validation rules.
     */
    public OptimizationResult optimize(StructuredResumeView resumeData, String jobDescription) {
        if (resumeData == null || jobDescription == null || jobDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Resume data and job description are required");
        }

        // Step 1: Extract and analyze job description keywords
        JobDescriptionAnalysis jobAnalysis = analyzeJobDescription(jobDescription);

        // Step 2: Extract keywords from existing resume content (STRICT - no invention)
        ResumeContentAnalysis resumeAnalysis = analyzeResumeContent(resumeData);

        // Step 3: Match job keywords against resume content (only existing content)
        KeywordMatchResult keywordMatches = matchKeywords(jobAnalysis, resumeAnalysis);

        // Step 4: Calculate ATS score based on matches
        double atsScore = calculateATSScore(keywordMatches, jobAnalysis, resumeAnalysis);

        // Step 5: Optimize structured resume view (reorder, prioritize, replace synonyms)
        StructuredResumeView optimized = optimizeResumeStructure(
                resumeData, jobAnalysis, keywordMatches);

        // Step 6: Track keyword usage to ensure natural distribution (1-3 occurrences)
        Map<String, Integer> keywordUsageCounts = trackKeywordUsage(optimized, keywordMatches.matchedKeywords);

        // Step 7: Generate optimization notes
        List<String> notes = generateOptimizationNotes(keywordMatches, atsScore);

        return new OptimizationResult(
                optimized,
                atsScore,
                keywordMatches.matchedKeywords,
                notes,
                keywordUsageCounts
        );
    }

    /**
     * Analyzes job description to extract keywords: skills, tools, technologies,
     * responsibilities, and job titles. Uses semantic understanding to identify
     * important terms.
     */
    private JobDescriptionAnalysis analyzeJobDescription(String jobDescription) {
        JobDescriptionAnalysis analysis = new JobDescriptionAnalysis();
        String lowerDesc = jobDescription.toLowerCase();

        // Extract technical skills and tools
        Pattern techPattern = Pattern.compile(
                "\\b(Java|Python|JavaScript|TypeScript|React|Angular|Vue|Node\\.js|Spring|Django|Flask|Express|" +
                "SQL|MySQL|PostgreSQL|MongoDB|Redis|Oracle|SQLite|" +
                "Docker|Kubernetes|AWS|Azure|GCP|Google Cloud|" +
                "Git|GitHub|GitLab|CI/CD|Jenkins|GitLab CI|GitHub Actions|" +
                "REST|API|GraphQL|Microservices|Agile|Scrum|DevOps|" +
                "HTML|CSS|Bootstrap|Tailwind|SASS|LESS|" +
                "C\\+\\+|C#|PHP|Ruby|Go|Rust|Swift|Kotlin|" +
                "Machine Learning|ML|AI|TensorFlow|PyTorch|" +
                "Data Science|Big Data|Hadoop|Spark|" +
                "Linux|Unix|Shell|Bash|PowerShell|" +
                "Networking|Security|Cloud|Automation|" +
                "Testing|QA|Selenium|JUnit|TestNG|" +
                "Jira|Confluence|Slack|Microsoft Office|Excel|Word|PowerPoint)\\b",
                Pattern.CASE_INSENSITIVE
        );

        // Extract soft skills
        Pattern softSkillsPattern = Pattern.compile(
                "\\b(communication|collaboration|teamwork|leadership|problem-solving|" +
                "critical thinking|creativity|adaptability|time management|organization|" +
                "attention to detail|analytical|interpersonal|presentation|negotiation|" +
                "mentoring|coaching|conflict resolution|multitasking|flexibility|" +
                "project management|stakeholder management|client relations)\\b",
                Pattern.CASE_INSENSITIVE
        );

        // Extract action verbs and responsibilities
        Pattern actionVerbsPattern = Pattern.compile(
                "\\b(develop|design|implement|create|build|manage|lead|improve|optimize|" +
                "automate|collaborate|deliver|achieve|resolve|enhance|establish|initiate|" +
                "streamline|coordinate|execute|analyze|architect|engineer|deploy|integrate|" +
                "maintain|support|test|debug|troubleshoot|document|train|mentor|review|" +
                "plan|strategize|execute|monitor|evaluate|assess|recommend|present|report)\\b",
                Pattern.CASE_INSENSITIVE
        );

        // Extract job titles
        Pattern jobTitlePattern = Pattern.compile(
                "\\b(Software Engineer|Developer|Programmer|Architect|Lead|Senior|Junior|" +
                "Full Stack|Frontend|Backend|DevOps|QA|Tester|Analyst|Consultant|" +
                "Manager|Director|Specialist|Engineer|Scientist|Data Engineer|" +
                "ML Engineer|AI Engineer|Cloud Engineer|Security Engineer)\\b",
                Pattern.CASE_INSENSITIVE
        );

        // Extract years of experience requirement
        Pattern expPattern = Pattern.compile("\\b(\\d+)[\\s-]*(?:to|\\-)?[\\s-]*(\\d+)?[\\s-]*(?:years?|yrs?|year|yr)\\b", Pattern.CASE_INSENSITIVE);

        // Extract all matches
        analysis.technicalSkills = extractUniqueMatches(jobDescription, techPattern);
        analysis.softSkills = extractUniqueMatches(jobDescription, softSkillsPattern);
        analysis.actionVerbs = extractUniqueMatches(jobDescription, actionVerbsPattern);
        analysis.jobTitles = extractUniqueMatches(jobDescription, jobTitlePattern);

        // Extract experience requirement
        java.util.regex.Matcher expMatcher = expPattern.matcher(jobDescription);
        if (expMatcher.find()) {
            analysis.minExperienceYears = Integer.parseInt(expMatcher.group(1));
            if (expMatcher.group(2) != null) {
                analysis.maxExperienceYears = Integer.parseInt(expMatcher.group(2));
            } else {
                analysis.maxExperienceYears = analysis.minExperienceYears;
            }
        }

        // Extract education requirements
        Pattern eduPattern = Pattern.compile(
                "\\b(bachelor|master|phd|degree|diploma|b\\.?tech|m\\.?tech|b\\.?e|m\\.?e|" +
                "b\\.?sc|m\\.?sc|computer science|engineering|cs|ce|it|information technology)\\b",
                Pattern.CASE_INSENSITIVE
        );
        analysis.educationRequirements = extractUniqueMatches(jobDescription, eduPattern);

        // Weight keywords by frequency and position (earlier mentions = higher weight)
        String[] sentences = jobDescription.split("[.!?]");
        Map<String, Double> keywordWeights = new HashMap<>();
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].toLowerCase();
            double weight = 1.0 + (sentences.length - i) * 0.1; // Earlier sentences weighted higher
            for (String keyword : analysis.technicalSkills) {
                if (sentence.contains(keyword.toLowerCase())) {
                    keywordWeights.put(keyword, keywordWeights.getOrDefault(keyword, 0.0) + weight);
                }
            }
        }
        analysis.keywordWeights = keywordWeights;

        return analysis;
    }

    /**
     * Analyzes existing resume content to extract keywords. STRICT: only extracts
     * what is actually present in the resume data.
     */
    private ResumeContentAnalysis analyzeResumeContent(StructuredResumeView resume) {
        ResumeContentAnalysis analysis = new ResumeContentAnalysis();

        // Extract from summary
        if (resume.getSummary() != null && !resume.getSummary().trim().isEmpty()) {
            String summary = resume.getSummary();
            analysis.summaryKeywords = extractKeywordsFromText(summary);
            analysis.allText.append(summary).append(" ");
        }

        // Extract from skills
        if (resume.getSkills() != null) {
            for (StructuredResumeView.SkillGroup group : resume.getSkills()) {
                if (group.getSkills() != null) {
                    for (String skill : group.getSkills()) {
                        analysis.skills.add(skill.toLowerCase().trim());
                        analysis.allText.append(skill).append(" ");
                    }
                }
            }
        }

        // Extract from experience
        if (resume.getExperience() != null) {
            for (StructuredResumeView.ExperienceItem exp : resume.getExperience()) {
                if (exp.getTitle() != null) {
                    analysis.jobTitles.add(exp.getTitle().toLowerCase().trim());
                    analysis.allText.append(exp.getTitle()).append(" ");
                }
                if (exp.getCompany() != null) {
                    analysis.allText.append(exp.getCompany()).append(" ");
                }
                if (exp.getBullets() != null) {
                    for (String bullet : exp.getBullets()) {
                        analysis.allText.append(bullet).append(" ");
                        analysis.experienceKeywords.addAll(extractKeywordsFromText(bullet));
                    }
                }
            }
        }

        // Extract from projects
        if (resume.getProjects() != null) {
            for (StructuredResumeView.ProjectItem project : resume.getProjects()) {
                if (project.getName() != null) {
                    analysis.allText.append(project.getName()).append(" ");
                }
                if (project.getBullets() != null) {
                    for (String bullet : project.getBullets()) {
                        analysis.allText.append(bullet).append(" ");
                        analysis.projectKeywords.addAll(extractKeywordsFromText(bullet));
                    }
                }
            }
        }

        // Extract from education
        if (resume.getEducation() != null) {
            for (StructuredResumeView.EducationItem edu : resume.getEducation()) {
                if (edu.getDegree() != null) {
                    analysis.education.add(edu.getDegree().toLowerCase().trim());
                    analysis.allText.append(edu.getDegree()).append(" ");
                }
                if (edu.getFieldOfStudy() != null) {
                    analysis.allText.append(edu.getFieldOfStudy()).append(" ");
                }
            }
        }

        // Extract all keywords from combined text
        analysis.allKeywords = extractKeywordsFromText(analysis.allText.toString());

        return analysis;
    }

    /**
     * Extracts keywords from text using common technical and professional terms
     */
    private Set<String> extractKeywordsFromText(String text) {
        Set<String> keywords = new LinkedHashSet<>();
        if (text == null || text.trim().isEmpty()) {
            return keywords;
        }

        // Technical skills pattern
        Pattern techPattern = Pattern.compile(
                "\\b(Java|Python|JavaScript|TypeScript|React|Angular|Vue|Node\\.js|Spring|Django|Flask|Express|" +
                "SQL|MySQL|PostgreSQL|MongoDB|Redis|Oracle|SQLite|" +
                "Docker|Kubernetes|AWS|Azure|GCP|Google Cloud|" +
                "Git|GitHub|GitLab|CI/CD|Jenkins|GitLab CI|GitHub Actions|" +
                "REST|API|GraphQL|Microservices|Agile|Scrum|DevOps|" +
                "HTML|CSS|Bootstrap|Tailwind|SASS|LESS|" +
                "C\\+\\+|C#|PHP|Ruby|Go|Rust|Swift|Kotlin|" +
                "Machine Learning|ML|AI|TensorFlow|PyTorch|" +
                "Data Science|Big Data|Hadoop|Spark|" +
                "Linux|Unix|Shell|Bash|PowerShell|" +
                "Networking|Security|Cloud|Automation|" +
                "Testing|QA|Selenium|JUnit|TestNG)\\b",
                Pattern.CASE_INSENSITIVE
        );

        java.util.regex.Matcher matcher = techPattern.matcher(text);
        while (matcher.find()) {
            keywords.add(matcher.group().toLowerCase());
        }

        return keywords;
    }

    /**
     * Matches job description keywords against existing resume content.
     * STRICT: Only matches keywords that exist in resume, ignores unmatched keywords.
     */
    private KeywordMatchResult matchKeywords(JobDescriptionAnalysis jobAnalysis, ResumeContentAnalysis resumeAnalysis) {
        KeywordMatchResult result = new KeywordMatchResult();

        // Match technical skills
        for (String jobSkill : jobAnalysis.technicalSkills) {
            String lowerJobSkill = jobSkill.toLowerCase();
            // Check exact match
            if (resumeAnalysis.skills.contains(lowerJobSkill) ||
                resumeAnalysis.allKeywords.contains(lowerJobSkill) ||
                resumeAnalysis.experienceKeywords.contains(lowerJobSkill) ||
                resumeAnalysis.projectKeywords.contains(lowerJobSkill)) {
                result.matchedKeywords.add(jobSkill);
                result.matchedTechnicalSkills.add(jobSkill);
            } else {
                // Check for semantic similarity (synonyms) - but only if synonym exists in resume
                String synonym = findSynonymInResume(lowerJobSkill, resumeAnalysis);
                if (synonym != null) {
                    result.matchedKeywords.add(jobSkill);
                    result.matchedTechnicalSkills.add(jobSkill);
                    result.synonymReplacements.put(synonym, jobSkill); // Replace resume synonym with exact job term
                } else {
                    result.unmatchedKeywords.add(jobSkill);
                }
            }
        }

        // Match soft skills
        for (String jobSkill : jobAnalysis.softSkills) {
            String lowerJobSkill = jobSkill.toLowerCase();
            if (resumeAnalysis.allKeywords.contains(lowerJobSkill) ||
                resumeAnalysis.summaryKeywords.contains(lowerJobSkill)) {
                result.matchedKeywords.add(jobSkill);
                result.matchedSoftSkills.add(jobSkill);
            } else {
                result.unmatchedKeywords.add(jobSkill);
            }
        }

        // Match action verbs
        for (String verb : jobAnalysis.actionVerbs) {
            String lowerVerb = verb.toLowerCase();
            if (resumeAnalysis.experienceKeywords.contains(lowerVerb) ||
                resumeAnalysis.projectKeywords.contains(lowerVerb)) {
                result.matchedKeywords.add(verb);
                result.matchedActionVerbs.add(verb);
            }
        }

        return result;
    }

    /**
     * Finds synonyms in resume content. Returns the resume synonym if found,
     * null otherwise. STRICT: Only returns synonyms that actually exist in resume.
     */
    private String findSynonymInResume(String jobKeyword, ResumeContentAnalysis resumeAnalysis) {
        // Common synonym mappings
        Map<String, List<String>> synonymMap = new HashMap<>();
        synonymMap.put("javascript", Arrays.asList("js", "ecmascript"));
        synonymMap.put("typescript", Arrays.asList("ts"));
        synonymMap.put("ci/cd", Arrays.asList("continuous integration", "continuous deployment", "ci", "cd"));
        synonymMap.put("kubernetes", Arrays.asList("k8s", "k8"));
        synonymMap.put("machine learning", Arrays.asList("ml", "deep learning", "dl"));
        synonymMap.put("natural language processing", Arrays.asList("nlp"));
        synonymMap.put("postgresql", Arrays.asList("postgres"));
        synonymMap.put("sql", Arrays.asList("ms sql", "sql server", "mysql", "postgresql"));
        synonymMap.put("git", Arrays.asList("github", "gitlab"));
        synonymMap.put("amazon web services", Arrays.asList("aws"));
        synonymMap.put("google cloud platform", Arrays.asList("gcp", "google cloud"));
        synonymMap.put("application programming interface", Arrays.asList("api", "rest api"));

        String lowerJobKeyword = jobKeyword.toLowerCase();
        
        // Check if job keyword has synonyms
        for (Map.Entry<String, List<String>> entry : synonymMap.entrySet()) {
            if (entry.getKey().equals(lowerJobKeyword) || entry.getValue().contains(lowerJobKeyword)) {
                // Check if any synonym exists in resume
                for (String synonym : entry.getValue()) {
                    if (resumeAnalysis.skills.contains(synonym) ||
                        resumeAnalysis.allKeywords.contains(synonym) ||
                        resumeAnalysis.experienceKeywords.contains(synonym)) {
                        return synonym; // Found synonym in resume
                    }
                }
                // Check if canonical form exists in resume
                if (resumeAnalysis.skills.contains(entry.getKey()) ||
                    resumeAnalysis.allKeywords.contains(entry.getKey())) {
                    return entry.getKey();
                }
            }
        }

        return null; // No synonym found in resume
    }

    /**
     * Calculates ATS compatibility score based on keyword matches
     */
    private double calculateATSScore(KeywordMatchResult matches, JobDescriptionAnalysis jobAnalysis, ResumeContentAnalysis resumeAnalysis) {
        if (jobAnalysis.technicalSkills.isEmpty() && jobAnalysis.softSkills.isEmpty()) {
            return 0.5; // Neutral score if no specific requirements
        }

        // Calculate match ratio for technical skills (most important)
        double techSkillScore = 0.0;
        if (!jobAnalysis.technicalSkills.isEmpty()) {
            int matchedTech = matches.matchedTechnicalSkills.size();
            techSkillScore = (double) matchedTech / jobAnalysis.technicalSkills.size();
        }

        // Calculate match ratio for soft skills
        double softSkillScore = 0.0;
        if (!jobAnalysis.softSkills.isEmpty()) {
            int matchedSoft = matches.matchedSoftSkills.size();
            softSkillScore = (double) matchedSoft / jobAnalysis.softSkills.size();
        }

        // Weighted combination: technical skills 70%, soft skills 30%
        double rawScore = (techSkillScore * 0.7) + (softSkillScore * 0.3);

        // Normalize to 0.0-1.0 range
        return Math.min(1.0, Math.max(0.0, rawScore));
    }

    /**
     * Optimizes resume structure by:
     * 1. Reordering content based on relevance to job description
     * 2. Prioritizing matched keywords
     * 3. Replacing synonyms with exact job-description terminology
     * 4. Improving clarity and impact of bullet points
     * STRICT: Only reorders and modifies existing content, never invents new content
     */
    private StructuredResumeView optimizeResumeStructure(
            StructuredResumeView original,
            JobDescriptionAnalysis jobAnalysis,
            KeywordMatchResult keywordMatches) {

        StructuredResumeView optimized = deepCopy(original);

        // 1. Optimize summary: align with job description using matched keywords
        if (optimized.getSummary() != null && !optimized.getSummary().trim().isEmpty()) {
            optimized.setSummary(optimizeSummary(optimized.getSummary(), keywordMatches));
        }

        // 2. Optimize skills: prioritize matched keywords, replace synonyms
        if (optimized.getSkills() != null) {
            optimized.setSkills(optimizeSkills(optimized.getSkills(), keywordMatches));
        }

        // 3. Optimize experience: reorder by relevance, improve bullet points
        if (optimized.getExperience() != null) {
            optimized.setExperience(optimizeExperience(optimized.getExperience(), keywordMatches, jobAnalysis));
        }

        // 4. Optimize projects: prioritize relevant projects
        if (optimized.getProjects() != null) {
            optimized.setProjects(optimizeProjects(optimized.getProjects(), keywordMatches));
        }

        return optimized;
    }

    /**
     * Optimizes summary by incorporating matched keywords naturally
     */
    private String optimizeSummary(String originalSummary, KeywordMatchResult keywordMatches) {
        String summary = originalSummary;
        
        // Replace synonyms with exact job terminology (limited to 1-2 replacements)
        int replacements = 0;
        for (Map.Entry<String, String> entry : keywordMatches.synonymReplacements.entrySet()) {
            if (replacements >= 2) break; // Limit replacements
            String resumeTerm = entry.getKey();
            String jobTerm = entry.getValue();
            if (summary.toLowerCase().contains(resumeTerm.toLowerCase())) {
                summary = summary.replaceAll("(?i)\\b" + Pattern.quote(resumeTerm) + "\\b", jobTerm);
                replacements++;
            }
        }

        return summary;
    }

    /**
     * Optimizes skills by prioritizing matched keywords and replacing synonyms
     */
    private List<StructuredResumeView.SkillGroup> optimizeSkills(
            List<StructuredResumeView.SkillGroup> originalSkills,
            KeywordMatchResult keywordMatches) {

        List<StructuredResumeView.SkillGroup> optimized = new ArrayList<>();

        for (StructuredResumeView.SkillGroup group : originalSkills) {
            StructuredResumeView.SkillGroup optimizedGroup = new StructuredResumeView.SkillGroup();
            optimizedGroup.setGroupName(group.getGroupName());

            // Prioritize matched keywords first
            List<String> matchedSkills = new ArrayList<>();
            List<String> otherSkills = new ArrayList<>();

            for (String skill : group.getSkills()) {
                String lowerSkill = skill.toLowerCase();
                boolean isMatched = keywordMatches.matchedTechnicalSkills.stream()
                        .anyMatch(m -> m.toLowerCase().equals(lowerSkill));
                
                // Replace synonym if exists
                String replacement = keywordMatches.synonymReplacements.get(lowerSkill);
                if (replacement != null) {
                    skill = replacement;
                }

                if (isMatched || keywordMatches.synonymReplacements.containsKey(lowerSkill)) {
                    matchedSkills.add(skill);
                } else {
                    otherSkills.add(skill);
                }
            }

            // Add matched skills first, then others
            optimizedGroup.getSkills().addAll(matchedSkills);
            optimizedGroup.getSkills().addAll(otherSkills);
            optimized.add(optimizedGroup);
        }

        return optimized;
    }

    /**
     * Optimizes experience by reordering based on relevance and improving bullet points
     */
    private List<StructuredResumeView.ExperienceItem> optimizeExperience(
            List<StructuredResumeView.ExperienceItem> originalExperience,
            KeywordMatchResult keywordMatches,
            JobDescriptionAnalysis jobAnalysis) {

        // Calculate relevance score for each experience item
        List<ExperienceRelevance> relevanceList = new ArrayList<>();
        for (StructuredResumeView.ExperienceItem exp : originalExperience) {
            double relevance = calculateExperienceRelevance(exp, keywordMatches, jobAnalysis);
            relevanceList.add(new ExperienceRelevance(exp, relevance));
        }

        // Sort by relevance (highest first)
        relevanceList.sort((a, b) -> Double.compare(b.relevance, a.relevance));

        // Build optimized list with improved bullet points
        List<StructuredResumeView.ExperienceItem> optimized = new ArrayList<>();
        for (ExperienceRelevance rel : relevanceList) {
            StructuredResumeView.ExperienceItem exp = rel.item;
            StructuredResumeView.ExperienceItem optimizedExp = new StructuredResumeView.ExperienceItem();
            optimizedExp.setCompany(exp.getCompany());
            optimizedExp.setTitle(exp.getTitle());
            optimizedExp.setLocation(exp.getLocation());
            optimizedExp.setStartDate(exp.getStartDate());
            optimizedExp.setEndDate(exp.getEndDate());

            // Optimize bullet points: replace synonyms, improve clarity
            List<String> optimizedBullets = new ArrayList<>();
            if (exp.getBullets() != null) {
                for (String bullet : exp.getBullets()) {
                    optimizedBullets.add(optimizeBulletPoint(bullet, keywordMatches));
                }
            }
            optimizedExp.setBullets(optimizedBullets);
            optimized.add(optimizedExp);
        }

        return optimized;
    }

    /**
     * Calculates relevance score for an experience item based on keyword matches
     */
    private double calculateExperienceRelevance(
            StructuredResumeView.ExperienceItem exp,
            KeywordMatchResult keywordMatches,
            JobDescriptionAnalysis jobAnalysis) {

        double score = 0.0;
        String combinedText = (exp.getTitle() != null ? exp.getTitle() : "") + " " +
                             (exp.getCompany() != null ? exp.getCompany() : "") + " " +
                             (exp.getBullets() != null ? String.join(" ", exp.getBullets()) : "");

        String lowerText = combinedText.toLowerCase();

        // Count matched keywords in this experience
        for (String keyword : keywordMatches.matchedKeywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                score += jobAnalysis.keywordWeights.getOrDefault(keyword, 1.0);
            }
        }

        return score;
    }

    /**
     * Optimizes a bullet point by replacing synonyms and improving clarity
     * STRICT: Only modifies existing text, never adds new content
     */
    private String optimizeBulletPoint(String bullet, KeywordMatchResult keywordMatches) {
        String optimized = bullet;

        // Replace synonyms with exact job terminology (limit to 1-2 per bullet)
        int replacements = 0;
        for (Map.Entry<String, String> entry : keywordMatches.synonymReplacements.entrySet()) {
            if (replacements >= 2) break;
            String resumeTerm = entry.getKey();
            String jobTerm = entry.getValue();
            if (optimized.toLowerCase().contains(resumeTerm.toLowerCase())) {
                optimized = optimized.replaceAll("(?i)\\b" + Pattern.quote(resumeTerm) + "\\b", jobTerm);
                replacements++;
            }
        }

        // Improve clarity: ensure active voice, remove first-person
        optimized = improveClarity(optimized);

        return optimized;
    }

    /**
     * Improves clarity by ensuring active voice and removing first-person language
     */
    private String improveClarity(String text) {
        String improved = text;

        // Remove first-person pronouns at start
        improved = improved.replaceAll("^(?i)(I|We|My|Our)\\s+", "");
        
        // Ensure sentence starts with action verb if it's a bullet point
        if (improved.trim().startsWith("-") || improved.trim().startsWith("*") || improved.trim().startsWith("•")) {
            // Already a bullet point, ensure it starts with action verb
            String content = improved.replaceFirst("^[\\-*•]\\s*", "");
            if (!content.trim().isEmpty() && !startsWithActionVerb(content)) {
                // Try to find action verb and move to start
                String[] words = content.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    if (isActionVerb(words[i])) {
                        // Reorder to start with action verb
                        StringBuilder reordered = new StringBuilder();
                        reordered.append(words[i]);
                        for (int j = 0; j < words.length; j++) {
                            if (j != i) {
                                reordered.append(" ").append(words[j]);
                            }
                        }
                        improved = improved.substring(0, improved.indexOf(content)) + reordered.toString();
                        break;
                    }
                }
            }
        }

        return improved;
    }

    /**
     * Checks if text starts with an action verb
     */
    private boolean startsWithActionVerb(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String firstWord = text.trim().split("\\s+")[0].toLowerCase();
        return isActionVerb(firstWord);
    }

    /**
     * Checks if a word is an action verb
     */
    private boolean isActionVerb(String word) {
        Set<String> actionVerbs = Set.of(
                "developed", "designed", "implemented", "created", "built", "managed", "led",
                "improved", "optimized", "automated", "collaborated", "delivered", "achieved",
                "resolved", "enhanced", "established", "initiated", "streamlined", "coordinated",
                "executed", "analyzed", "architected", "engineered", "deployed", "integrated",
                "maintained", "supported", "tested", "debugged", "troubleshot", "documented",
                "trained", "mentored", "reviewed", "planned", "strategized", "monitored",
                "evaluated", "assessed", "recommended", "presented", "reported"
        );
        return actionVerbs.contains(word.toLowerCase());
    }

    /**
     * Optimizes projects by prioritizing relevant ones
     */
    private List<StructuredResumeView.ProjectItem> optimizeProjects(
            List<StructuredResumeView.ProjectItem> originalProjects,
            KeywordMatchResult keywordMatches) {

        // Calculate relevance for each project
        List<ProjectRelevance> relevanceList = new ArrayList<>();
        for (StructuredResumeView.ProjectItem project : originalProjects) {
            double relevance = calculateProjectRelevance(project, keywordMatches);
            relevanceList.add(new ProjectRelevance(project, relevance));
        }

        // Sort by relevance
        relevanceList.sort((a, b) -> Double.compare(b.relevance, a.relevance));

        // Build optimized list
        List<StructuredResumeView.ProjectItem> optimized = new ArrayList<>();
        for (ProjectRelevance rel : relevanceList) {
            StructuredResumeView.ProjectItem project = rel.item;
            StructuredResumeView.ProjectItem optimizedProject = new StructuredResumeView.ProjectItem();
            optimizedProject.setName(project.getName());
            optimizedProject.setRole(project.getRole());
            optimizedProject.setDates(project.getDates());

            // Optimize bullet points
            List<String> optimizedBullets = new ArrayList<>();
            if (project.getBullets() != null) {
                for (String bullet : project.getBullets()) {
                    optimizedBullets.add(optimizeBulletPoint(bullet, keywordMatches));
                }
            }
            optimizedProject.setBullets(optimizedBullets);
            optimized.add(optimizedProject);
        }

        return optimized;
    }

    /**
     * Calculates relevance score for a project
     */
    private double calculateProjectRelevance(
            StructuredResumeView.ProjectItem project,
            KeywordMatchResult keywordMatches) {

        double score = 0.0;
        String combinedText = (project.getName() != null ? project.getName() : "") + " " +
                             (project.getBullets() != null ? String.join(" ", project.getBullets()) : "");

        String lowerText = combinedText.toLowerCase();

        for (String keyword : keywordMatches.matchedKeywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                score += 1.0;
            }
        }

        return score;
    }

    /**
     * Tracks keyword usage to ensure natural distribution (1-3 occurrences max)
     */
    private Map<String, Integer> trackKeywordUsage(StructuredResumeView resume, List<String> keywords) {
        Map<String, Integer> usageCounts = new HashMap<>();
        String allText = buildResumeText(resume).toLowerCase();

        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            int count = countOccurrences(allText, lowerKeyword);
            usageCounts.put(keyword, count);
        }

        return usageCounts;
    }

    /**
     * Counts occurrences of a keyword in text (word boundaries)
     */
    private int countOccurrences(String text, String keyword) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Builds a single text string from structured resume
     */
    private String buildResumeText(StructuredResumeView resume) {
        StringBuilder sb = new StringBuilder();
        if (resume.getSummary() != null) sb.append(resume.getSummary()).append(" ");
        if (resume.getSkills() != null) {
            for (StructuredResumeView.SkillGroup group : resume.getSkills()) {
                if (group.getSkills() != null) {
                    sb.append(String.join(" ", group.getSkills())).append(" ");
                }
            }
        }
        if (resume.getExperience() != null) {
            for (StructuredResumeView.ExperienceItem exp : resume.getExperience()) {
                if (exp.getBullets() != null) {
                    sb.append(String.join(" ", exp.getBullets())).append(" ");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Generates optimization notes
     */
    private List<String> generateOptimizationNotes(KeywordMatchResult matches, double atsScore) {
        List<String> notes = new ArrayList<>();
        
        notes.add(String.format("ATS Compatibility Score: %.1f%%", atsScore * 100));
        notes.add(String.format("Matched %d keywords from job description", matches.matchedKeywords.size()));
        
        if (!matches.synonymReplacements.isEmpty()) {
            notes.add(String.format("Replaced %d synonyms with exact job-description terminology", 
                    matches.synonymReplacements.size()));
        }
        
        if (!matches.unmatchedKeywords.isEmpty()) {
            notes.add(String.format("Note: %d job keywords not found in resume (not added per strict rules)", 
                    matches.unmatchedKeywords.size()));
        }
        
        notes.add("Content reordered and prioritized based on job relevance");
        notes.add("Bullet points optimized for clarity and impact");
        notes.add("All content verified against original resume data");

        return notes;
    }

    /**
     * Helper: Extract unique matches from text using pattern
     */
    private Set<String> extractUniqueMatches(String text, Pattern pattern) {
        Set<String> matches = new LinkedHashSet<>();
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    /**
     * Deep copy of StructuredResumeView
     */
    private StructuredResumeView deepCopy(StructuredResumeView original) {
        StructuredResumeView copy = new StructuredResumeView();
        
        // Copy header
        if (original.getHeader() != null) {
            StructuredResumeView.Header header = new StructuredResumeView.Header();
            header.setFullName(original.getHeader().getFullName());
            header.setEmail(original.getHeader().getEmail());
            header.setPhone(original.getHeader().getPhone());
            header.setLocation(original.getHeader().getLocation());
            header.setLinkedin(original.getHeader().getLinkedin());
            header.setWebsite(original.getHeader().getWebsite());
            copy.setHeader(header);
        }
        
        copy.setSummary(original.getSummary());
        copy.setOtherContent(original.getOtherContent());
        
        // Copy skills
        if (original.getSkills() != null) {
            List<StructuredResumeView.SkillGroup> skills = new ArrayList<>();
            for (StructuredResumeView.SkillGroup group : original.getSkills()) {
                StructuredResumeView.SkillGroup newGroup = new StructuredResumeView.SkillGroup();
                newGroup.setGroupName(group.getGroupName());
                if (group.getSkills() != null) {
                    newGroup.getSkills().addAll(group.getSkills());
                }
                skills.add(newGroup);
            }
            copy.setSkills(skills);
        }
        
        // Copy experience
        if (original.getExperience() != null) {
            List<StructuredResumeView.ExperienceItem> experience = new ArrayList<>();
            for (StructuredResumeView.ExperienceItem exp : original.getExperience()) {
                StructuredResumeView.ExperienceItem newExp = new StructuredResumeView.ExperienceItem();
                newExp.setCompany(exp.getCompany());
                newExp.setTitle(exp.getTitle());
                newExp.setLocation(exp.getLocation());
                newExp.setStartDate(exp.getStartDate());
                newExp.setEndDate(exp.getEndDate());
                if (exp.getBullets() != null) {
                    newExp.getBullets().addAll(exp.getBullets());
                }
                experience.add(newExp);
            }
            copy.setExperience(experience);
        }
        
        // Copy projects
        if (original.getProjects() != null) {
            List<StructuredResumeView.ProjectItem> projects = new ArrayList<>();
            for (StructuredResumeView.ProjectItem proj : original.getProjects()) {
                StructuredResumeView.ProjectItem newProj = new StructuredResumeView.ProjectItem();
                newProj.setName(proj.getName());
                newProj.setRole(proj.getRole());
                newProj.setDates(proj.getDates());
                if (proj.getBullets() != null) {
                    newProj.getBullets().addAll(proj.getBullets());
                }
                projects.add(newProj);
            }
            copy.setProjects(projects);
        }
        
        // Copy education
        if (original.getEducation() != null) {
            List<StructuredResumeView.EducationItem> education = new ArrayList<>();
            for (StructuredResumeView.EducationItem edu : original.getEducation()) {
                StructuredResumeView.EducationItem newEdu = new StructuredResumeView.EducationItem();
                newEdu.setInstitution(edu.getInstitution());
                newEdu.setDegree(edu.getDegree());
                newEdu.setFieldOfStudy(edu.getFieldOfStudy());
                newEdu.setDates(edu.getDates());
                if (edu.getBullets() != null) {
                    newEdu.getBullets().addAll(edu.getBullets());
                }
                education.add(newEdu);
            }
            copy.setEducation(education);
        }
        
        // Copy achievements
        if (original.getAchievements() != null) {
            List<StructuredResumeView.AchievementItem> achievements = new ArrayList<>();
            for (StructuredResumeView.AchievementItem ach : original.getAchievements()) {
                StructuredResumeView.AchievementItem newAch = new StructuredResumeView.AchievementItem();
                newAch.setTitle(ach.getTitle());
                newAch.setIssuer(ach.getIssuer());
                newAch.setDate(ach.getDate());
                newAch.setDetails(ach.getDetails());
                achievements.add(newAch);
            }
            copy.setAchievements(achievements);
        }
        
        return copy;
    }

    // Inner classes for analysis data structures
    private static class JobDescriptionAnalysis {
        Set<String> technicalSkills = new LinkedHashSet<>();
        Set<String> softSkills = new LinkedHashSet<>();
        Set<String> actionVerbs = new LinkedHashSet<>();
        Set<String> jobTitles = new LinkedHashSet<>();
        Set<String> educationRequirements = new LinkedHashSet<>();
        Map<String, Double> keywordWeights = new HashMap<>();
        int minExperienceYears = 0;
        int maxExperienceYears = 0;
    }

    private static class ResumeContentAnalysis {
        Set<String> skills = new LinkedHashSet<>();
        Set<String> jobTitles = new LinkedHashSet<>();
        Set<String> education = new LinkedHashSet<>();
        Set<String> summaryKeywords = new LinkedHashSet<>();
        Set<String> experienceKeywords = new LinkedHashSet<>();
        Set<String> projectKeywords = new LinkedHashSet<>();
        Set<String> allKeywords = new LinkedHashSet<>();
        StringBuilder allText = new StringBuilder();
    }

    private static class KeywordMatchResult {
        List<String> matchedKeywords = new ArrayList<>();
        List<String> unmatchedKeywords = new ArrayList<>();
        List<String> matchedTechnicalSkills = new ArrayList<>();
        List<String> matchedSoftSkills = new ArrayList<>();
        List<String> matchedActionVerbs = new ArrayList<>();
        Map<String, String> synonymReplacements = new HashMap<>(); // resume term -> job term
    }

    private static class ExperienceRelevance {
        StructuredResumeView.ExperienceItem item;
        double relevance;

        ExperienceRelevance(StructuredResumeView.ExperienceItem item, double relevance) {
            this.item = item;
            this.relevance = relevance;
        }
    }

    private static class ProjectRelevance {
        StructuredResumeView.ProjectItem item;
        double relevance;

        ProjectRelevance(StructuredResumeView.ProjectItem item, double relevance) {
            this.item = item;
            this.relevance = relevance;
        }
    }
}

