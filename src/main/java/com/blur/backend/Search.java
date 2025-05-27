package com.blur.backend;

import java.io.Serializable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

import com.google.gson.annotations.Expose;

public class Search implements Serializable {
    private transient final Term term;

    @Expose
    private final String id;
    private final ConcurrentSkipListSet<String> urls;
    private Status status;

    private transient final Pattern pattern;
    private transient final ConcurrentSkipListSet<String> searchedUrls;

    public Search(String id, Term term) {
        this.term = term;
        this.id = id;
        this.urls = new ConcurrentSkipListSet<>();
        this.status = Status.ACTIVE;
        this.searchedUrls = new ConcurrentSkipListSet<>();

        // Create a pattern that matches the whole word, including special characters
        String keywordRegex = term.getKeyword().toLowerCase().replaceAll("([\\\\+\\[\\]{}()^$.|?*])", "\\\\$1");
        this.pattern = Pattern.compile("(?<![\\w+])(" + keywordRegex + ")(?![\\w+])", Pattern.CASE_INSENSITIVE);        
    }

    public Term getTerm() {
        return term;
    }

    public String getId() {
        return id;
    }

    public ConcurrentSkipListSet<String> getUrls() {
        return urls;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ConcurrentSkipListSet<String> getSearchedUrls() {
        return searchedUrls;
    }

    public void addUrl(String url) {
        urls.add(url);
    }

    public synchronized boolean addSearchedUrl(String url) {
        return this.searchedUrls.add(url);
    }

    public boolean containsWholeKeyword(String text) {
        return text != null ? this.pattern.matcher(text).find() : false;
    }
}
