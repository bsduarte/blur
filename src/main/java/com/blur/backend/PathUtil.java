package com.blur.backend;

import java.util.List;

public class PathUtil {
    private static final List<String> FILE_EXTENSIONS = List.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".rar", ".tar", ".gz", ".bz2", ".7z",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".mp3", ".wav", ".mp4", ".avi", ".mov"
    );


    public static String normalizePath(String path) {
        // Split URL into base and path parts
        String basePart = "";
        String pathPart = path;
        
        if (path.startsWith("http://") || path.startsWith("https://")) {
            int firstSlash =  path.indexOf('/', path.startsWith("https://") ? 8 : 7);
            if (firstSlash != -1) {
                basePart = path.substring(0, firstSlash);
                pathPart = path.substring(firstSlash);
            } else {
                return path; // No path part to normalize
            }
        }

        String[] parts = pathPart.split("/");
        StringBuilder cleanedPath = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            else if (!part.equals("..")) {
                cleanedPath.append("/").append(part);
            } else {
                // Remove the last part if ".." is found
                if (cleanedPath.length() > 0) {
                    int lastSlashIndex = cleanedPath.lastIndexOf("/");
                    if (lastSlashIndex != -1) {
                        cleanedPath.setLength(lastSlashIndex);
                    }
                }
            }
        }

        // Ensure at least a root slash exists
        if (cleanedPath.length() == 0) {
            cleanedPath.append("/");
        }
        return basePart + cleanedPath.toString();
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
