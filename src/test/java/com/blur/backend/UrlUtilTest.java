package com.blur.backend;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class UrlUtilTest {
    @Test
    void testGetNormalizedUrl() throws Exception {
        // Test normal URL
        String normalUrl = "http://example.com/path";
        URL result = UrlUtil.getNormalizedUrl(normalUrl);
        assertEquals(normalUrl, result.toString());

        // Test URL with spaces
        String urlWithSpaces = "http://example.com/path with spaces";
        result = UrlUtil.getNormalizedUrl(urlWithSpaces);
        assertEquals("http://example.com/path%20with%20spaces", result.toString());

        // Test URL with pipe character
        String urlWithPipe = "http://example.com/path|with|pipes";
        result = UrlUtil.getNormalizedUrl(urlWithPipe);
        assertEquals("http://example.com/path%7Cwith%7Cpipes", result.toString());

        // Test URL with new line character
        String urlWithNewLine = "https://www.brighttalk.com/webcast/6793/514263\n" + //
                        "?utm_medium=blog&amp;utm_campaign=7014K000000UV0j";
        result = UrlUtil.getNormalizedUrl(urlWithNewLine);
        assertEquals("https://www.brighttalk.com/webcast/6793/514263%0A" + //
                        "?utm_medium=blog&amp;utm_campaign=7014K000000UV0j", result.toString());

        // Test invalid URL
        String invalidUrl = "not a valid url";
        assertThrows(RuntimeException.class, () -> {
            UrlUtil.getNormalizedUrl(invalidUrl);
        });
    }

    @Test
    void testIsFileLink() {
        // Test various file extensions
        assertTrue(UrlUtil.isFileLink("document.pdf"));
        assertTrue(UrlUtil.isFileLink("image.jpg"));
        assertTrue(UrlUtil.isFileLink("archive.zip"));
        assertTrue(UrlUtil.isFileLink("http://example.com/file.mp4"));
        
        // Test non-file links
        assertFalse(UrlUtil.isFileLink("http://example.com"));
        assertFalse(UrlUtil.isFileLink("http://example.com/page"));
        assertFalse(UrlUtil.isFileLink("http://example.com/path/"));
    }

    @Test
    void testExtractLinks() {
        // Test basic link extraction
        String content = "<a href=\"http://example.com\">Link</a>" +
                        "<a href='http://test.com'>Test</a>" +
                        "<a href=http://plain.com>Plain</a>";
        
        List<String> links = UrlUtil.extractLinks(content, null, false)
                                  .collect(Collectors.toList());
        
        assertEquals(3, links.size());
        assertTrue(links.contains("http://example.com"));
        assertTrue(links.contains("http://test.com"));
        assertTrue(links.contains("http://plain.com"));

        // Test exclusion patterns
        List<String> exclusions = Arrays.asList("test.com", "plain.com");
        links = UrlUtil.extractLinks(content, exclusions, false)
                      .collect(Collectors.toList());
        
        assertEquals(1, links.size());
        assertTrue(links.contains("http://example.com"));

        // Test file link exclusion
        content = "<a href=\"http://example.com/doc.pdf\">PDF</a>" +
                 "<a href=\"http://example.com/page.html\">HTML</a>";
        
        links = UrlUtil.extractLinks(content, null, true)
                      .collect(Collectors.toList());
        
        assertEquals(1, links.size());
        assertTrue(links.contains("http://example.com/page.html"));
    }

    @Test
    void testGetNormalizedAbsoluteUrl() throws Exception {
        URL rootPage = (new URI("http://example.com")).toURL();
        URL currentPage = (new URI("http://example.com/path/page.html")).toURL();

        // Test absolute URL
        URL result = UrlUtil.getNormalizedAbsoluteUrl(
            rootPage, 
            currentPage, 
            "http://other.com/test.html"
        );
        assertEquals("http://other.com/test.html", result.toString());

        // Test relative URL
        result = UrlUtil.getNormalizedAbsoluteUrl(
            rootPage,
            currentPage,
            "subpage.html"
        );
        assertEquals("http://example.com/path/subpage.html", result.toString());

        // Test root-relative URL
        result = UrlUtil.getNormalizedAbsoluteUrl(
            rootPage,
            currentPage,
            "/rootpath/page.html"
        );
        assertEquals("http://example.com/rootpath/page.html", result.toString());

        // Test current URL with no trailing slash
        currentPage = (new URI("http://example.com/path/current")).toURL();
        result = UrlUtil.getNormalizedAbsoluteUrl(
            rootPage,
            currentPage,
            "relative.html"
        );
        assertEquals("http://example.com/path/relative.html", result.toString());
    }
}