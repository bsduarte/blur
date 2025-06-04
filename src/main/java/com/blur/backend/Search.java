package com.blur.backend;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Search implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Search.class);
    
    private static final String ROOT_PAGE_REGEX = "^(https?://[a-zA-Z0-9.-]+(?:\\:\\d+)?).*";
    private static List<String> exclusionPatterns = List.of("#", "javascript:", "mailto:");

    @Expose
    private final String id;

    private final Term term;
    private final URL baseUrl;
    private final Instant createdAt;
    private Status status;

    @SerializedName("urls")
    private final ConcurrentSkipListSet<String> resultUrls;

    private final transient URL rootPage;
    private final transient ConcurrentSkipListSet<String> searchedUrls;

    public Search(Term term, URL baseUrl) {
        this.term = Objects.requireNonNull(term);
        this.baseUrl = Objects.requireNonNull(baseUrl);
        try {
            rootPage = (new URI(baseUrl.toString().replaceAll(ROOT_PAGE_REGEX, "$1"))).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalStateException("Invalid BASE_URL format.");
        }
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.status = Status.CREATED;
        this.createdAt = Instant.now();
        this.resultUrls = new ConcurrentSkipListSet<>();
        this.searchedUrls = new ConcurrentSkipListSet<>();
    }

    public void start() {
        if (this.status != Status.CREATED) {
            throw new IllegalStateException("Search can't be started. Current status is " + this.status + ".");
        }
        this.status = Status.ACTIVE;
        addSearchedUrl(baseUrl.toString());
        Thread.ofVirtual().start(() -> crawl(baseUrl, true));
    }

    public Term getTerm() {
        return this.term;
    }

    public String getId() {
        return this.id;
    }

    public Status getStatus() {
        return this.status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public ConcurrentSkipListSet<String> getResultUrls() {
        return this.resultUrls;
    }

    public ConcurrentSkipListSet<String> getSearchedUrls() {
        return this.searchedUrls;
    }

    synchronized boolean addSearchedUrl(String url) {
        return this.searchedUrls.add(url);
    }

    private void setStatus(Status status) {
        this.status = status;
    }

    private void crawl(URL url, boolean isRoot) {
        logger.info("Crawling URL: {}", url);
        try {
            String content = HttpClientUtil.fetchContent(url);
            if (this.term.isContainedAsWholeKeyword(content)) {
                this.resultUrls.add(url.toString());
            }
            List<Thread> threads = new ArrayList<>();

            // Loop by URLs in the content
            UrlUtil.extractLinks(content, exclusionPatterns, true).forEach(link -> {
                // Getting normalized URLs avoid duplicates
                URL normalizedUrl = UrlUtil.getNormalizedAbsoluteUrl(rootPage, url, link);

                String normalizedUrlStr = normalizedUrl.toString();
                if (normalizedUrlStr.startsWith(baseUrl.toString()) && addSearchedUrl(normalizedUrlStr)) {
                    logger.debug("Found URL: {} -> Normalized URL: {}", link, normalizedUrlStr);
                    threads.add(Thread.ofVirtual().start(() -> crawl(normalizedUrl, false)));
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
            logger.error("Error crawling URL: {}", url);
            logger.debug(ExceptionUtils.getStackTrace(e), e);
        } finally {
            if (isRoot) {
                setStatus(Status.DONE);
                logger.info("Crawling completed for search ID: {} [keyword:{}]", getId(), getTerm().getKeyword());
            }
        }
    }    
}
