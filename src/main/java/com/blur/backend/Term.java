package com.blur.backend;

import java.util.regex.Pattern;

public class Term {
    private final String keyword;
    private transient Pattern pattern;

    public Term(String keyword) {
        if (keyword.length() < 4) {
            throw new IllegalArgumentException("Keyword must be at least 4 characters long");
        }
        if (keyword.length() > 32) {
            throw new IllegalArgumentException("Keyword must be at most 32 characters long");
        }        
        this.keyword = keyword;
    }

    public String getKeyword() {
        return this.keyword;
    }

    public boolean isContainedAsWholeKeyword(String text) {
        if (this.pattern == null) {
            compilePattern();
        }
        return text != null ? this.pattern.matcher(text).find() : false;
    }

    private void compilePattern() {
        // Create a pattern that matches the whole word, including special characters
        String keywordRegex = this.keyword.toLowerCase().replaceAll("([\\\\+\\[\\]{}()^$.|?*])", "\\\\$1");
        this.pattern = Pattern.compile("(?<![\\w+])(" + keywordRegex + ")(?![\\w+])", Pattern.CASE_INSENSITIVE);
    }
}
