package com.blur.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathUtilTest {

    @Test
    void testNormalizePath() {
        // Test case for current directory
        assertEquals("https://example.com/path/file.html", 
            PathUtil.normalizePath("https://example.com/path/./file.html"));
        
        // Test case for parent directory
        assertEquals("https://example.com/file.html", 
            PathUtil.normalizePath("https://example.com/path/../file.html"));
        
        // Test case for multiple parent directories
        assertEquals("https://example.com/final.html", 
            PathUtil.normalizePath("https://example.com/path/to/../../final.html"));
        
        // Test case for absolute paths
        assertEquals("https://example.com/absolute.html", 
            PathUtil.normalizePath("https://example.com/path/to/../../../../absolute.html"));
        
        // Test case for no normalization needed
        assertEquals("https://example.com/normal/path.html", 
            PathUtil.normalizePath("https://example.com/normal/path.html"));
    }

    @Test
    void testIsFileLink() {
        // Test various file extensions
        assertTrue(PathUtil.isFileLink("document.pdf"));
        assertTrue(PathUtil.isFileLink("image.jpg"));
        assertTrue(PathUtil.isFileLink("archive.zip"));
        assertTrue(PathUtil.isFileLink("http://example.com/file.mp4"));
        
        // Test non-file links
        assertFalse(PathUtil.isFileLink("http://example.com"));
        assertFalse(PathUtil.isFileLink("http://example.com/page"));
        assertFalse(PathUtil.isFileLink("http://example.com/path/"));
    }
}