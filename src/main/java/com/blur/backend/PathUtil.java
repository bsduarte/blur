package com.blur.backend;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathUtil {
    private static final Logger logger = LoggerFactory.getLogger(PathUtil.class);

    private static final List<String> FILE_EXTENSIONS = List.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".rar", ".tar", ".gz", ".bz2", ".7z",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".mp3", ".wav", ".mp4", ".avi", ".mov"
    );

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
