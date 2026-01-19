package com.resumeopt.service;

import com.resumeopt.model.ResumeChange;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ResumeDiffService {

    /**
     * Generate detailed change list comparing original and optimized text
     * Only tracks meaningful changes (words/phrases that actually changed)
     */
    public List<ResumeChange> generateChanges(String originalText, String optimizedText, com.resumeopt.model.Resume resume) {
        List<ResumeChange> changes = new ArrayList<>();
        
        if (originalText == null) originalText = "";
        if (optimizedText == null) optimizedText = "";
        
        // If texts are identical, no changes
        if (originalText.trim().equals(optimizedText.trim())) {
            return changes;
        }
        
        // Use a simpler approach: split by words and compare
        List<WordToken> origWords = tokenizeWords(originalText, 0);
        List<WordToken> optWords = tokenizeWords(optimizedText, 0);
        
        // Use a sliding window approach to find changes
        int origIdx = 0, optIdx = 0;
        
        while (origIdx < origWords.size() || optIdx < optWords.size()) {
            WordToken origWord = origIdx < origWords.size() ? origWords.get(origIdx) : null;
            WordToken optWord = optIdx < optWords.size() ? optWords.get(optIdx) : null;
            
            if (origWord != null && optWord != null) {
                String origNorm = normalizeWord(origWord.text);
                String optNorm = normalizeWord(optWord.text);
                
                if (origNorm.equals(optNorm)) {
                    // Words match, move forward
                    origIdx++;
                    optIdx++;
                } else {
                    // Words differ - check if it's a meaningful change
                    if (isMeaningfulWord(origWord.text) && isMeaningfulWord(optWord.text)) {
                        ResumeChange change = createChange(ResumeChange.ChangeType.MODIFY,
                            origWord.text, optWord.text,
                            origWord.startPos, origWord.endPos,
                            optWord.startPos, optWord.endPos,
                            originalText, resume);
                        if (change != null) changes.add(change);
                    }
                    origIdx++;
                    optIdx++;
                }
            } else if (origWord != null) {
                // Word deleted
                if (isMeaningfulWord(origWord.text)) {
                    ResumeChange change = createChange(ResumeChange.ChangeType.DELETE,
                        origWord.text, null,
                        origWord.startPos, origWord.endPos,
                        null, null,
                        originalText, resume);
                    if (change != null) changes.add(change);
                }
                origIdx++;
            } else if (optWord != null) {
                // Word inserted
                if (isMeaningfulWord(optWord.text)) {
                    ResumeChange change = createChange(ResumeChange.ChangeType.INSERT,
                        null, optWord.text,
                        null, null,
                        optWord.startPos, optWord.endPos,
                        optimizedText, resume);
                    if (change != null) changes.add(change);
                }
                optIdx++;
            }
        }
        
        // Filter out trivial changes and merge consecutive changes
        changes = filterTrivialChanges(changes);
        changes = mergeConsecutiveChanges(changes);
        
        return changes;
    }
    
    /**
     * Merge consecutive changes in the same section
     */
    private List<ResumeChange> mergeConsecutiveChanges(List<ResumeChange> changes) {
        if (changes.size() <= 1) return changes;
        
        List<ResumeChange> merged = new ArrayList<>();
        ResumeChange current = changes.get(0);
        
        for (int i = 1; i < changes.size(); i++) {
            ResumeChange next = changes.get(i);
            
            // Check if changes are consecutive and in same section
            boolean consecutive = false;
            if (current.getEndPosition() != null && next.getStartPosition() != null) {
                int gap = next.getStartPosition() - current.getEndPosition();
                consecutive = gap >= 0 && gap <= 10; // Within 10 characters
            }
            
            boolean sameSection = current.getSection() != null && 
                                 current.getSection().equals(next.getSection());
            
            if (consecutive && sameSection && current.getChangeType() == next.getChangeType()) {
                // Merge changes
                if (current.getChangeType() == ResumeChange.ChangeType.MODIFY) {
                    // Merge modify changes
                    current.setOriginalText(
                        (current.getOriginalText() != null ? current.getOriginalText() : "") + 
                        " " + (next.getOriginalText() != null ? next.getOriginalText() : ""));
                    current.setNewText(
                        (current.getNewText() != null ? current.getNewText() : "") + 
                        " " + (next.getNewText() != null ? next.getNewText() : ""));
                    if (next.getEndPosition() != null) {
                        current.setEndPosition(next.getEndPosition());
                    }
                    if (next.getNewEndPosition() != null) {
                        current.setNewEndPosition(next.getNewEndPosition());
                    }
                    current.setDescription(createDescription(current.getChangeType(), 
                        current.getOriginalText(), current.getNewText()));
                } else if (current.getChangeType() == ResumeChange.ChangeType.DELETE) {
                    current.setOriginalText(
                        (current.getOriginalText() != null ? current.getOriginalText() : "") + 
                        " " + (next.getOriginalText() != null ? next.getOriginalText() : ""));
                    if (next.getEndPosition() != null) {
                        current.setEndPosition(next.getEndPosition());
                    }
                    current.setDescription(createDescription(current.getChangeType(), 
                        current.getOriginalText(), null));
                } else if (current.getChangeType() == ResumeChange.ChangeType.INSERT) {
                    current.setNewText(
                        (current.getNewText() != null ? current.getNewText() : "") + 
                        " " + (next.getNewText() != null ? next.getNewText() : ""));
                    if (next.getNewEndPosition() != null) {
                        current.setNewEndPosition(next.getNewEndPosition());
                    }
                    current.setDescription(createDescription(current.getChangeType(), 
                        null, current.getNewText()));
                }
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        
        return merged;
    }
    
    /**
     * Tokenize text into words with positions
     */
    private List<WordToken> tokenizeWords(String text, int basePos) {
        List<WordToken> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        
        Pattern wordPattern = Pattern.compile("\\b\\w+\\b");
        java.util.regex.Matcher matcher = wordPattern.matcher(text);
        
        while (matcher.find()) {
            tokens.add(new WordToken(matcher.group(), basePos + matcher.start(), basePos + matcher.end()));
        }
        
        return tokens;
    }
    
    /**
     * Create a ResumeChange object
     */
    private ResumeChange createChange(ResumeChange.ChangeType type, String originalText, String newText,
            Integer startPos, Integer endPos, Integer newStartPos, Integer newEndPos,
            String fullText, com.resumeopt.model.Resume resume) {
        
        ResumeChange change = new ResumeChange();
        change.setResume(resume);
        change.setChangeType(type);
        change.setOriginalText(originalText);
        change.setNewText(newText);
        change.setStartPosition(startPos);
        change.setEndPosition(endPos);
        change.setNewStartPosition(newStartPos);
        change.setNewEndPosition(newEndPos);
        
        // Detect section
        int pos = startPos != null ? startPos : (newStartPos != null ? newStartPos : 0);
        change.setSection(detectSection(fullText, pos));
        
        // Create description
        String desc = createDescription(type, originalText, newText);
        change.setDescription(desc);
        
        return change;
    }
    
    /**
     * Create human-readable description
     */
    private String createDescription(ResumeChange.ChangeType type, String original, String newText) {
        switch (type) {
            case INSERT:
                return "Added: \"" + truncate(newText, 40) + "\"";
            case DELETE:
                return "Removed: \"" + truncate(original, 40) + "\"";
            case MODIFY:
                return "Changed: \"" + truncate(original, 25) + "\" → \"" + truncate(newText, 25) + "\"";
            default:
                return "Change";
        }
    }
    
    /**
     * Check if a word is meaningful (not just punctuation/whitespace)
     */
    private boolean isMeaningfulWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        // Must have at least one letter or digit
        return word.matches(".*[a-zA-Z0-9].*");
    }
    
    /**
     * Filter out trivial changes
     */
    private List<ResumeChange> filterTrivialChanges(List<ResumeChange> changes) {
        List<ResumeChange> filtered = new ArrayList<>();
        
        for (ResumeChange change : changes) {
            // Skip if only whitespace/punctuation changed
            if (change.getOriginalText() != null && change.getNewText() != null) {
                String origNorm = normalizeWord(change.getOriginalText());
                String newNorm = normalizeWord(change.getNewText());
                if (origNorm.equals(newNorm)) {
                    continue; // Skip trivial changes
                }
            }
            
            // Skip if original or new text is just whitespace
            if (change.getOriginalText() != null && change.getOriginalText().trim().isEmpty()) {
                if (change.getChangeType() == ResumeChange.ChangeType.DELETE) {
                    continue; // Skip whitespace-only deletions
                }
            }
            if (change.getNewText() != null && change.getNewText().trim().isEmpty()) {
                if (change.getChangeType() == ResumeChange.ChangeType.INSERT) {
                    continue; // Skip whitespace-only insertions
                }
            }
            
            filtered.add(change);
        }
        
        return filtered;
    }
    
    /**
     * Normalize word for comparison
     */
    private String normalizeWord(String word) {
        if (word == null) return "";
        return word.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
    
    /**
     * Detect which section a position belongs to
     */
    private String detectSection(String text, int position) {
        if (text == null || position < 0 || position >= text.length()) {
            return "Other";
        }
        
        String before = text.substring(0, Math.min(position, text.length()));
        String[] sectionKeywords = {
            "skills", "experience", "education", "summary", "objective", 
            "projects", "certifications", "achievements", "references", "work"
        };
        
        // Look for section headers
        for (String keyword : sectionKeywords) {
            Pattern pattern = Pattern.compile("(?i)^.*\\b" + keyword + "\\s*:?\\s*$", Pattern.MULTILINE);
            java.util.regex.Matcher matcher = pattern.matcher(before);
            int lastMatchPos = -1;
            while (matcher.find()) {
                lastMatchPos = matcher.end();
            }
            if (lastMatchPos > 0 && position >= lastMatchPos) {
                // Check if we haven't passed another section
                boolean found = true;
                for (String otherKeyword : sectionKeywords) {
                    if (!otherKeyword.equals(keyword)) {
                        Pattern otherPattern = Pattern.compile("(?i)^.*\\b" + otherKeyword + "\\s*:?\\s*$", Pattern.MULTILINE);
                        java.util.regex.Matcher otherMatcher = otherPattern.matcher(before);
                        int otherPos = -1;
                        while (otherMatcher.find()) {
                            otherPos = otherMatcher.end();
                        }
                        if (otherPos > lastMatchPos) {
                            found = false;
                            break;
                        }
                    }
                }
                if (found) {
                    return capitalize(keyword);
                }
            }
        }
        
        // Default based on position
        if (position < text.length() * 0.15) {
            return "Header";
        } else if (position < text.length() * 0.4) {
            return "Summary";
        } else if (position < text.length() * 0.7) {
            return "Experience";
        } else {
            return "Other";
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
    
    // Helper class
    private static class WordToken {
        String text;
        int startPos;
        int endPos;
        
        WordToken(String text, int startPos, int endPos) {
            this.text = text;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }
}
