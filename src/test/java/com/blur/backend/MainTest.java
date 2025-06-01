package com.blur.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class MainTest extends BaseTest {
    private static final Map<String, String> testPages = new HashMap<>();
    private static final Map<String, String> relativePages = new HashMap<>();
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .create();

    @BeforeAll
    static void startServer() {
        // Setup test pages with links
        testPages.put("/test/", 
            "<html><body>\n" +
            "    <a href=\"/test/page1.html\">Page 1</a>\n" +
            "    <a href=\"/test/page2.html\">Page 2</a>\n" +
            "    <a href=\"/test/subdir/page3.html\">Page 3</a>\n" +
            "    <a href=\"http://external.com\">External</a>\n" +
            "    test content here\n" +
            "</body></html>");
            
        testPages.put("/test/page1.html",
            "<html><body>\n" +
            "    <a href=\"/page2.html\">To Page 2</a>\n" +
            "    <a href=\"/files/doc.pdf\">PDF</a>\n" +
            "    More test content\n" +
            "</body></html>");
            
        testPages.put("/test/page2.html",
            "<html><body>\n" +
            "    <a href=\"/page1.html\">Back to Page 1</a>\n" +
            "    <a href=\"/nonexistent.html\">404 Link</a>\n" +
            "    No test keyword here\n" +
            "</body></html>");
            
        testPages.put("/test/subdir/page3.html",
            "<html><body>\n" +
            "    <a href=\"../page1.html\">Up to Page 1</a>\n" +
            "    Contains test word\n" +
            "</body></html>");

        relativePages.put("/test/",
        "<html><body>\n" +
        "    <a href=\"./page1.html\">Page 1</a>\n" +
        "    <a href=\"subdir/../page2.html\">Page 2</a>\n" +
        "    <a href=\"subdir/./page3.html\">Page 3</a>\n" +
        "    test keyword here\n" +
        "</body></html>");
        relativePages.put("/test/page1.html", testPages.get("/test/page1.html"));
        relativePages.put("/test/page2.html", testPages.get("/test/page2.html"));
        relativePages.put("/test/subdir/page3.html", testPages.get("/test/subdir/page3.html"));
    }

    @BeforeEach
    void setUp() {
        HttpClientUtil.shutdown();
    }

    @AfterEach
    void tearDown() {
        TestUtil.stopTestServer();
        System.clearProperty("BASE_URL");
    }

    @Test
    void testCompleteCrawl() throws Exception {
        // Create test server with dynamic routing
        String baseUrlStr = TestUtil.createTestServer((req, res) -> {
            res.type("text/html");
            return testPages.get(req.pathInfo());
        });
        runMain(baseUrlStr);
        
        // Create new search using HTTP POST
        String searchJson = "{\"keyword\":\"test\"}";
        TestResponse res = testPost(TestUtil.serverAddr(), "/crawl", searchJson);
        assertEquals(200, res.status);
        Search search = gson.fromJson(res.body, Search.class);
        assertNotNull(search);
        
        // Wait for crawling to complete
        boolean isDone = false;
        for (int i = 0; i < 10 && !isDone; i++) {
            TimeUnit.SECONDS.sleep(1);
            String statusResponse = HttpClientUtil.fetchContent(TestUtil.serverAddr() + "/crawl/" + search.getId());
            Search completedSearch = gson.fromJson(statusResponse, Search.class);
            if (completedSearch.getStatus() == Status.DONE) {
                isDone = true;
                ConcurrentSkipListSet<String> urls = completedSearch.getUrls();
                assertTrue(urls.contains(baseUrlStr));
                assertTrue(urls.contains(baseUrlStr + "page1.html"));
                assertTrue(urls.contains(baseUrlStr + "page2.html"));
                assertTrue(urls.contains(baseUrlStr + "subdir/page3.html"));
            }
        }
        assertTrue(isDone, "Search did not complete within timeout");
    }

    @Test
    void testPathNormalization() throws Exception {
        String baseUrlStr = TestUtil.createTestServer((req, res) -> {
            res.type("text/html");
            return relativePages.get(req.pathInfo());
        });
        runMain(baseUrlStr);

        // Create new search
        String searchJson = "{\"keyword\":\"test\"}";
        TestResponse res = testPost(TestUtil.serverAddr(), "/crawl", searchJson);
        assertEquals(200, res.status);
        Search search = gson.fromJson(res.body, Search.class);
        
        // Wait for crawling to complete
        boolean isDone = false;
        for (int i = 0; i < 10 && !isDone; i++) {
            TimeUnit.SECONDS.sleep(1);
            String statusResponse = HttpClientUtil.fetchContent(TestUtil.serverAddr() + "/crawl/" + search.getId());
            Search completedSearch = gson.fromJson(statusResponse, Search.class);
            if (completedSearch.getStatus() == Status.DONE) {
                isDone = true;
                ConcurrentSkipListSet<String> urls = completedSearch.getUrls();
                assertTrue(urls.contains(baseUrlStr + "page1.html"));
                assertTrue(urls.contains(baseUrlStr + "page2.html"));
                assertTrue(urls.contains(baseUrlStr + "subdir/page3.html"));
            }
        }
        assertTrue(isDone, "Search did not complete within timeout");
    }

    private void runMain(String baseUrl) {
        System.setProperty("BASE_URL", baseUrl);
        Main.setTestMode(true);
        Main.main(new String[]{});
    }

    private TestResponse testPost(String baseUrl, String path, String body) throws Exception {
        try {
            // Remove any trailing slash from baseUrl
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            // Ensure path starts with slash
            path = path.startsWith("/") ? path : "/" + path;
            
            URI uri = URI.create(baseUrl + path);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "*/*");
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            connection.connect();
            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode <= 300 ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            connection.disconnect();

            return new TestResponse(responseCode, responseBody);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestResponse {
        public final int status;
        public final String body;

        public TestResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}