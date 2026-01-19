package com.resumeopt.model;

/**
 * Enum representing ATS-friendly HTML/PDF template styles.
 */
public enum ResumeTemplateStyle {
    MINIMAL,
    PROFESSIONAL,
    MODERN,
    TECH,
    FRESHER;

    public static ResumeTemplateStyle fromString(String value) {
        if (value == null) return MINIMAL;
        String v = value.trim().toUpperCase();
        for (ResumeTemplateStyle style : values()) {
            if (style.name().equals(v)) {
                return style;
            }
        }
        return MINIMAL;
    }
}


