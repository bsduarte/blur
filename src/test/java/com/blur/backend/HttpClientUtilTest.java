package com.blur.backend;

import org.junit.jupiter.api.Test;

import com.blur.backend.HttpClientUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

class HttpClientUtilTest extends BaseTest {
    private String testServerUrl;
    private static final String TEST_CONTENT = "Test content";
    
    @BeforeEach
    void setUp() {
        HttpClientUtil.shutdown(); // Reset client state
        testServerUrl = TestUtil.createTestServer(TEST_CONTENT);
    }

    @AfterEach
    void stopServer() {
        TestUtil.stopTestServer();
    }

    @Test
    void testFetchContentSuccess() throws Exception {
        String content = HttpClientUtil.fetchContent(testServerUrl);
        assertNotNull(content);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    void testFetchContentInvalidUrl() {
        assertThrows(Exception.class, () -> {
            HttpClientUtil.getContent("http://invalid.url");
        });
    }

    @Test
    void testFetchContentWithSpaces() throws Exception {
        String urlWithSpaces = testServerUrl + "/path with spaces";
        String content = HttpClientUtil.fetchContent(urlWithSpaces);
        assertNotNull(content);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    void testFetchContentWithRetry() throws Exception {
        // Create a server that will fail twice then succeed
        int failureCount = 2;
        String flakeyServerUrl = TestUtil.createTestServerWithFailures(TEST_CONTENT, failureCount, 500);
        
        for (int i = 0; i < failureCount; i++) {
            assertThrows(Exception.class, () -> {
                HttpClientUtil.getContent(flakeyServerUrl);
            });
        }
        
        String content = HttpClientUtil.fetchContent(flakeyServerUrl);
        assertNotNull(content);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    void testFetchContentWithTimeout() throws Exception {
        String slowServerUrl = TestUtil.createTestServerWithDelay(TEST_CONTENT, 12000); // 12 second delay
        assertThrows(Exception.class, () -> {
            HttpClientUtil.getContent(slowServerUrl);
        });
    }

    @Test
    void testConcurrentRequests() throws Exception {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        Exception[] exceptions = new Exception[threadCount];
        
        String testUrl = TestUtil.createTestServer(TEST_CONTENT);
        
        for (int i = 0; i < threadCount; i++) {
            int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    String content = HttpClientUtil.fetchContent(testUrl);
                    assertEquals(TEST_CONTENT, content);
                } catch (Exception e) {
                    exceptions[threadId] = e;
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        for (Exception exception : exceptions) {
            assertNull(exception, "No exceptions should occur during concurrent requests");
        }
    }

    @Test
    void testCookieHandling() throws Exception {
        String cookieServerUrl = TestUtil.createTestServer((req, res) -> {
            String cookieHeader = req.headers("Cookie");
            if (cookieHeader == null) {
                res.header("Set-Cookie", "sessionId=id123");
                return "First request";
            }
            return "Request with cookies: " + cookieHeader;
        });
        
        // First request should get cookies but not send any
        String firstResponse = HttpClientUtil.fetchContent(cookieServerUrl);
        assertEquals("First request", firstResponse);
        
        // Second request should not send cookies (cookie store is empty)
        String secondResponse = HttpClientUtil.fetchContent(cookieServerUrl);
        assertEquals("First request", secondResponse);
    }
}