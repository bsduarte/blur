package com.blur.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;

class PathUtilTest {

    @Test
    void testGetNormalizedUrl() throws Exception {
        // Test normal URL
        String normalUrl = "http://example.com/path";
        URL result = PathUtil.getNormalizedUrl(normalUrl);
        assertEquals(normalUrl, result.toString());

        // Test URL with spaces
        String urlWithSpaces = "http://example.com/path with spaces";
        result = PathUtil.getNormalizedUrl(urlWithSpaces);
        assertEquals("http://example.com/path%20with%20spaces", result.toString());

        // Test URL with pipe character
        String urlWithPipe = "http://example.com/path|with|pipes";
        result = PathUtil.getNormalizedUrl(urlWithPipe);
        assertEquals("http://example.com/path%7Cwith%7Cpipes", result.toString());

        // Test URL with new line character
        String urlWithNewLine = "https://www.brighttalk.com/webcast/6793/514263\n" + //
                        "?utm_medium=blog&amp;utm_campaign=7014K000000UV0j";
        result = PathUtil.getNormalizedUrl(urlWithNewLine);
        assertEquals("https://www.brighttalk.com/webcast/6793/514263%0A" + //
                        "?utm_medium=blog&amp;utm_campaign=7014K000000UV0j", result.toString());

        // Test invalid URL
        String invalidUrl = "not a valid url";
        assertThrows(RuntimeException.class, () -> {
            PathUtil.getNormalizedUrl(invalidUrl);
        });
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