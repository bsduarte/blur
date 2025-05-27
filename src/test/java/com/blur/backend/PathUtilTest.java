package com.blur.backend;

import org.junit.jupiter.api.Test;

import com.blur.backend.Main;

import static org.junit.jupiter.api.Assertions.*;

class PathUtilTest {

    @Test
    void testNormalizePath() {
        // Test case for current directory
        assertEquals("https://example.com/path/file.html", 
            Main.normalizePath("https://example.com/path/./file.html"));
        
        // Test case for parent directory
        assertEquals("https://example.com/file.html", 
            Main.normalizePath("https://example.com/path/../file.html"));
        
        // Test case for multiple parent directories
        assertEquals("https://example.com/final.html", 
            Main.normalizePath("https://example.com/path/to/../../final.html"));
        
        // Test case for absolute paths
        assertEquals("https://example.com/absolute.html", 
            Main.normalizePath("https://example.com/path/to/../../../../absolute.html"));
        
        // Test case for no normalization needed
        assertEquals("https://example.com/normal/path.html", 
            Main.normalizePath("https://example.com/normal/path.html"));
    }

    @Test
    void testIsFileLink() {
        // Test various file extensions
        assertTrue(Main.isFileLink("document.pdf"));
        assertTrue(Main.isFileLink("image.jpg"));
        assertTrue(Main.isFileLink("archive.zip"));
        assertTrue(Main.isFileLink("http://example.com/file.mp4"));
        
        // Test non-file links
        assertFalse(Main.isFileLink("http://example.com"));
        assertFalse(Main.isFileLink("http://example.com/page"));
        assertFalse(Main.isFileLink("http://example.com/path/"));
    }
}