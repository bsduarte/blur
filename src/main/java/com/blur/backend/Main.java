package com.blur.backend;

import static spark.Spark.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    private static final JsonTransformer jsonTransformer = new JsonTransformer();
    private static final JsonTransformerExpose jsonTransformerExpose = new JsonTransformerExpose();
    private static final Gson gson = new Gson();
    private static final ConcurrentHashMap<String, Search> searches = new ConcurrentHashMap<>();

    private static final Pattern URL_PATTERN = Pattern.compile("<a\\b[^>]*?href\\s*=\\s*(?:[\"']([^\"']*)[\"']|([^\\s>]*))");
    private static final String ROOT_PAGE_REGEX = "^(https?://[a-zA-Z0-9.-]+(?:\\:\\d+)?).*";

    private static final List<String> FILE_EXTENSIONS = List.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".rar", ".tar", ".gz", ".bz2", ".7z",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".mp3", ".wav", ".mp4", ".avi", ".mov"
    );

    private static String baseUrl;
    private static String rootPage;
    private static boolean isTestMode = false;

    public static void setTestMode(boolean testMode) {
        isTestMode = testMode;
    }

    public static void main(String[] args) {
        try {
            initialize();
            setupRoutes();
            setupShutdownHook();
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            if (!isTestMode) {
                System.exit(1);
            }
        }
    }

    private static void initialize() {
        baseUrl = System.getProperty("BASE_URL", System.getenv("BASE_URL"));
        if (baseUrl == null) {
            throw new IllegalStateException("BASE_URL environment variable is not set.");
        }
        rootPage = baseUrl.replaceAll(ROOT_PAGE_REGEX, "$1");
        if (rootPage == null) {
            throw new IllegalStateException("Invalid BASE_URL format.");
        }
    }

    private static void setupRoutes() {
        before((request, response) -> {
            response.type("application/json");
        });

        get("/crawl/:id", (req, res) -> {
            String id = req.params("id");
            Search search = searches.get(id);
            if (search == null) {
                halt(404, "Search " + id + " not found");
            }
            return search;
        }, jsonTransformer);

        post("/crawl", (req, res) -> {
            Term term = gson.fromJson(req.body(), Term.class);
            if (term.getKeyword().length() < 4) {
                halt(400, "Keyword must be at least 4 characters long");
            }
            if (term.getKeyword().length() > 32) {
                halt(400, "Keyword must be at most 32 characters long");
            }

            String id = UUID.randomUUID().toString().substring(0, 8);
            Search search = new Search(id, term);
            searches.put(id, search);
            
            // Start crawling in a new thread
            search.addSearchedUrl(baseUrl);
            new Thread(() -> crawl(baseUrl, search, true)).start();
            
            return search;
        }, jsonTransformerExpose);
    }    

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            HttpClientUtil.shutdown();
        }));
    }

    private static void crawl(String url, Search search, boolean isRoot) {
        logger.info("Crawling URL: {}", url);
        try {
            String content = HttpClientUtil.fetchContent(url);
            if (search.containsWholeKeyword(content)) {
                search.addUrl(url);
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
                    || isFileLink(foundHref)) {
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
                String normalizedUrl = (!foundUrl.contains("/./") && !foundUrl.contains("/../")) ? foundUrl : normalizePath(foundUrl);
                if (normalizedUrl.startsWith(baseUrl) && search.addSearchedUrl(normalizedUrl)) {
                    Thread thread = new Thread(() -> {
                        crawl(normalizedUrl, search, false);
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
                search.setStatus(Status.DONE);
                logger.info("Crawling completed for search ID: {}[keyword:{}]", search.getId(), search.getTerm().getKeyword());
            }
        }
    }    

    static String normalizePath(String path) {
        // Split URL into base and path parts
        String basePart = "";
        String pathPart = path;
        
        if (path.startsWith("http://") || path.startsWith("https://")) {
            int firstSlash =  path.indexOf('/', path.startsWith("https://") ? 8 : 7);
            if (firstSlash != -1) {
                basePart = path.substring(0, firstSlash);
                pathPart = path.substring(firstSlash);
            } else {
                return path; // No path part to normalize
            }
        }

        String[] parts = pathPart.split("/");
        StringBuilder cleanedPath = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            else if (!part.equals("..")) {
                cleanedPath.append("/").append(part);
            } else {
                // Remove the last part if ".." is found
                if (cleanedPath.length() > 0) {
                    int lastSlashIndex = cleanedPath.lastIndexOf("/");
                    if (lastSlashIndex != -1) {
                        cleanedPath.setLength(lastSlashIndex);
                    }
                }
            }
        }

        // Ensure at least a root slash exists
        if (cleanedPath.length() == 0) {
            cleanedPath.append("/");
        }
        return basePart + cleanedPath.toString();
    }

    static boolean isFileLink(String url) {
        String lowerUrl = (url.contains("?") ? url.substring(0, url.indexOf("?")) : url).toLowerCase();
        for (String ext : FILE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
