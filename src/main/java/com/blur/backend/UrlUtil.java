package com.blur.backend;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlUtil {
    private static final Logger logger = LoggerFactory.getLogger(UrlUtil.class);

    private static final Pattern URL_PATTERN = Pattern.compile("<a\\b[^>]*?href\\s*=\\s*(?:[\"']([^\"']*)[\"']|([^\\s>]*))");
    private static final List<String> FILE_EXTENSIONS = List.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".rar", ".tar", ".gz", ".bz2", ".7z",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".mp3", ".wav", ".mp4", ".avi", ".mov"
    );

    public static Stream<String> extractLinks(String content, List<String> exclusionPatterns, boolean excludeFileLinks) {
        return URL_PATTERN.matcher(content).results()
                .map(match -> match.group(1) != null ? match.group(1) : match.group(2))
                .filter(
                        link -> link != null && !link.isEmpty()
                        && (!excludeFileLinks || !isFileLink(link))
                        && (exclusionPatterns == null || exclusionPatterns.isEmpty() || exclusionPatterns.stream().noneMatch(link::contains)));
                //group1 != null ? group1 : match.group(2); // Handle both quoted and unquoted matches
    }

    public static URL getNormalizedAbsoluteUrl(URL rootPage, URL url, String link) {
        String linkUrlStr;
        if (link.startsWith("http://") || link.startsWith("https://")) {
            linkUrlStr = link;
        } else {
            // Handle relative URLs properly
            if (link.startsWith("/")) {
                // Absolute path from domain root
                linkUrlStr = rootPage.toString() + link;
            } else {
                // Relative path from current URL
                String urlStr = url.toString();
                linkUrlStr = (urlStr.endsWith("/") ? urlStr : urlStr.substring(0, urlStr.lastIndexOf("/") + 1)) + link;
            }
        }
        return UrlUtil.getNormalizedUrl(linkUrlStr);
    }

    public static URL getNormalizedUrl(String url) {
        try {
            return (new URI(url.replaceAll(" ", "%20").replaceAll("\n", "%0A").replaceAll("\\|", "%7C"))).normalize().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            logger.error("Malformed URL: {}", url);
            logger.debug(ExceptionUtils.getStackTrace(e), e);
            throw new RuntimeException("Malformed URL: " + url, e);
        }
    }

    public static boolean isFileLink(String url) {
        String lowerUrl = (url.contains("?") ? url.substring(0, url.indexOf("?")) : url).toLowerCase();
        for (String ext : FILE_EXTENSIONS) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
