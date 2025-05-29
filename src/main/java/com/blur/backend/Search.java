package com.blur.backend;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.Expose;

public class Search implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Search.class);

    private static final Pattern URL_PATTERN = Pattern.compile("<a\\b[^>]*?href\\s*=\\s*(?:[\"']([^\"']*)[\"']|([^\\s>]*))");
    private static final String ROOT_PAGE_REGEX = "^(https?://[a-zA-Z0-9.-]+(?:\\:\\d+)?).*";

    @Expose
    private final String id;
    private final Term term;
    private final String baseUrl;
    private final Instant createdAt;
    private Status status;
    private final ConcurrentSkipListSet<String> urls;

    private final transient String rootPage;
    private final transient ConcurrentSkipListSet<String> searchedUrls;

    public Search(Term term, String baseUrl) {
        this.term = term;
        this.baseUrl = baseUrl;
        rootPage = baseUrl.replaceAll(ROOT_PAGE_REGEX, "$1");
        if (rootPage == null) {
            throw new IllegalStateException("Invalid BASE_URL format.");
        }        
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.urls = new ConcurrentSkipListSet<>();
        this.status = Status.CREATED;
        this.createdAt = Instant.now();
        this.searchedUrls = new ConcurrentSkipListSet<>();
    }

    public void start() {
        if (this.status != Status.CREATED) {
            throw new IllegalStateException("Search can't be started. Current status is " + this.status + ".");
        }
        this.status = Status.ACTIVE;
        addSearchedUrl(baseUrl);
        new Thread(() -> crawl(baseUrl, true)).start();
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

    public ConcurrentSkipListSet<String> getSearchedUrls() {
        return searchedUrls;
    }

    private void addUrl(String url) {
        urls.add(url);
    }

    private synchronized boolean addSearchedUrl(String url) {
        return this.searchedUrls.add(url);
    }

    private void setStatus(Status status) {
        this.status = status;
    }

    private void crawl(String url, boolean isRoot) {
        logger.info("Crawling URL: {}", url);
        try {
            String content = HttpClientUtil.fetchContent(url);
            if (this.term.isContainedAsWholeKeyword(content)) {
                addUrl(url);
            }
            List<Thread> threads = new ArrayList<>();

            // Loop by URLs in the content
            Matcher urlMatcher = URL_PATTERN.matcher(content);
            urlMatcher.results().forEach(match -> {
                String group1 = match.group(1);
                String foundHref = group1 != null ? group1 : match.group(2); // Handle both quoted and unquoted matches
                if (foundHref == null || foundHref.trim().isEmpty() 
                    || foundHref.contains("#") 
                    || foundHref.contains("javascript:") 
                    || foundHref.contains("mailto:")
                    || PathUtil.isFileLink(foundHref)) {
                    // Skip empty, fragment, javascript, and mailto links
                    return;
                }
                
                String foundUrl;
                if (foundHref.startsWith("http://") || foundHref.startsWith("https://")) {
                    foundUrl = foundHref;
                } else {
                    // Handle relative URLs properly
                    if (foundHref.startsWith("/")) {
                        // Absolute path from domain root
                        foundUrl = rootPage + foundHref;
                    } else {
                        // Relative path from current URL
                        foundUrl = (url.endsWith("/") ? url : url.substring(0, url.lastIndexOf("/") + 1)) + foundHref;
                    }
                }

                // Normalize the URL to avoid duplicates
                String normalizedUrl = (!foundUrl.contains("/./") && !foundUrl.contains("/../")) ? foundUrl : PathUtil.normalizePath(foundUrl);
                if (normalizedUrl.startsWith(baseUrl) && addSearchedUrl(normalizedUrl)) {
                    Thread thread = new Thread(() -> {
                        crawl(normalizedUrl, false);
                    });
                    threads.add(thread);
                    thread.start();
                }
            });
            // Wait for all threads to finish
            try {
                for (Thread thread : threads) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                logger.error("Thread interrupted while waiting for sub-thread to finish", e);
                Thread.currentThread().interrupt(); // Restore the interrupted status
            }            
        } catch (Exception e) {
            logger.error("Error crawling URL: {}. {}", url, e.getMessage());
            logger.debug("Error crawling URL: {}", url, e);
        } finally {
            if (isRoot) {
                setStatus(Status.DONE);
                logger.info("Crawling completed for search ID: {}[keyword:{}]", getId(), getTerm().getKeyword());
            }
        }
    }    
}
