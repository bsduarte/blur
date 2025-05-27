package com.blur.backend;

import org.junit.jupiter.api.Test;

import com.blur.backend.Search;
import com.blur.backend.Status;
import com.blur.backend.Term;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

class SearchTest {
    private Search search;
    private Term term;
    private static final String TEST_ID = "test123";
    private static final String TEST_KEYWORD = "test";
    
    @BeforeEach
    void setUp() {
        term = new Term(TEST_KEYWORD);
        search = new Search(TEST_ID, term);
    }

    @Test
    void testSearchInitialization() {
        assertEquals(TEST_ID, search.getId());
        assertEquals(TEST_KEYWORD, search.getTerm().getKeyword());
        assertEquals(Status.ACTIVE, search.getStatus());
        assertTrue(search.getUrls().isEmpty());
        assertTrue(search.getSearchedUrls().isEmpty());
    }

    @Test
    void testAddUrl() {
        String testUrl = "https://example.com";
        search.addUrl(testUrl);
        assertTrue(search.getUrls().contains(testUrl));
    }

    @Test
    void testAddSearchedUrl() {
        String testUrl = "https://example.com";
        assertTrue(search.addSearchedUrl(testUrl));
        assertTrue(search.getSearchedUrls().contains(testUrl));
        // Test duplicate URL
        assertFalse(search.addSearchedUrl(testUrl));
    }

    @Test
    void testContainsWholeKeyword() {
        assertTrue(search.containsWholeKeyword("this is a test message"));
        assertTrue(search.containsWholeKeyword("TEST"));
        assertFalse(search.containsWholeKeyword("testing"));
        assertFalse(search.containsWholeKeyword("contest"));
    }

    @Test
    void testStatusTransition() {
        assertEquals(Status.ACTIVE, search.getStatus());
        search.setStatus(Status.DONE);
        assertEquals(Status.DONE, search.getStatus());
    }

    @Test
    void testConcurrentUrlAddition() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    search.addUrl("https://example.com/page" + index);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount, search.getUrls().size());
    }

    @Test
    void testConcurrentSearchedUrlAddition() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();
        List<Boolean> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    boolean added = search.addSearchedUrl("https://example.com/page" + index);
                    synchronized(results) {
                        results.add(added);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount, search.getSearchedUrls().size());
        assertEquals(threadCount, results.stream().filter(r -> r).count());
    }

    @Test
    void testKeywordMatchingWithSpecialCharacters() {
        Term specialTerm = new Term("c++");
        Search specialSearch = new Search("test", specialTerm);
        
        assertTrue(specialSearch.containsWholeKeyword("this is c++ code"));
        assertTrue(specialSearch.containsWholeKeyword("C++"));
        assertFalse(specialSearch.containsWholeKeyword("c+"));
        assertFalse(specialSearch.containsWholeKeyword("c+++"));
    }
}