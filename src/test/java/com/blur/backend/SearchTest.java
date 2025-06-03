package com.blur.backend;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.TimeUnit;
// import java.util.ArrayList;
// import java.util.List;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

class SearchTest {
    private Search search;
    private Term term;
    private static final String TEST_KEYWORD = "test";
    private static URL TEST_URL = null;
    
    @BeforeEach
    void setUp() {
        try {
            TEST_URL = new URI("https://example.com").toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
        }
        term = new Term(TEST_KEYWORD);
        search = new Search(term, TEST_URL);
    }

    @Test
    void testSearchInitialization() {
        assertTrue(search.getId() != null && search.getId().length() > 0);
        assertEquals(TEST_KEYWORD, search.getTerm().getKeyword());
        assertEquals(Status.CREATED, search.getStatus());
        assertTrue(search.getUrls().isEmpty());
        assertTrue(search.getSearchedUrls().isEmpty());
    }

    @Test
    void testAddSearchedUrl() {
        search.start();
        assertTrue(search.getSearchedUrls().contains(TEST_URL.toString()));
    }

    @Test
    void testContainsWholeKeyword() {
        assertTrue(search.getTerm().isContainedAsWholeKeyword("this is a test message"));
        assertTrue(search.getTerm().isContainedAsWholeKeyword("TEST"));
        assertFalse(search.getTerm().isContainedAsWholeKeyword("testing"));
        assertFalse(search.getTerm().isContainedAsWholeKeyword("contest"));
    }

    @Test
    void testStatusTransition() {
        assertEquals(Status.CREATED, search.getStatus());
        search.start();
        assertNotEquals(Status.CREATED, search.getStatus());
    }

    // TODO: Find a way to uncomment and implement these tests(of private methods)
    // @Test
    // void testConcurrentUrlAddition() throws InterruptedException {
    //     int threadCount = 10;
    //     CountDownLatch startLatch = new CountDownLatch(1);
    //     CountDownLatch doneLatch = new CountDownLatch(threadCount);
    //     List<Thread> threads = new ArrayList<>();

    //     for (int i = 0; i < threadCount; i++) {
    //         final int index = i;
    //         Thread thread = new Thread(() -> {
    //             try {
    //                 startLatch.await();
    //                 search.addUrl("https://example.com/page" + index);
    //             } catch (InterruptedException e) {
    //                 Thread.currentThread().interrupt();
    //             } finally {
    //                 doneLatch.countDown();
    //             }
    //         });
    //         threads.add(thread);
    //         thread.start();
    //     }

    //     startLatch.countDown();
    //     assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
    //     assertEquals(threadCount, search.getUrls().size());
    // }

    // @Test
    // void testConcurrentSearchedUrlAddition() throws InterruptedException {
    //     int threadCount = 10;
    //     CountDownLatch startLatch = new CountDownLatch(1);
    //     CountDownLatch doneLatch = new CountDownLatch(threadCount);
    //     List<Thread> threads = new ArrayList<>();
    //     List<Boolean> results = new ArrayList<>();

    //     for (int i = 0; i < threadCount; i++) {
    //         final int index = i;
    //         Thread thread = new Thread(() -> {
    //             try {
    //                 startLatch.await();
    //                 boolean added = search.addSearchedUrl("https://example.com/page" + index);
    //                 synchronized(results) {
    //                     results.add(added);
    //                 }
    //             } catch (InterruptedException e) {
    //                 Thread.currentThread().interrupt();
    //             } finally {
    //                 doneLatch.countDown();
    //             }
    //         });
    //         threads.add(thread);
    //         thread.start();
    //     }
    //
    //     startLatch.countDown();
    //     assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
    //     assertEquals(threadCount, search.getSearchedUrls().size());
    //     assertEquals(threadCount, results.stream().filter(r -> r).count());
    // }

    @Test
    void testKeywordMatchingWithSpecialCharacters() {
        Term specialTerm = new Term("c/c++");
        Search specialSearch = new Search(specialTerm, TEST_URL);
        
        assertTrue(specialSearch.getTerm().isContainedAsWholeKeyword("this is c/c++ code"));
        assertTrue(specialSearch.getTerm().isContainedAsWholeKeyword("C/C++"));
        assertFalse(specialSearch.getTerm().isContainedAsWholeKeyword("c+"));
        assertFalse(specialSearch.getTerm().isContainedAsWholeKeyword("c+++"));
    }
}