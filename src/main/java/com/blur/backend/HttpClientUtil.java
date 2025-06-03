package com.blur.backend;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static final int MAX_RESULT_QUEUED_PER_DESTINATION = 4096;
    private static final long CONNECTION_TIMEOUT = 1000;
    private static final long REQUEST_TIMEOUT = 10000;

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private static final int MAX_RETRY_DELAY_MS = 10000;

    private static volatile HttpClient httpClient;
    private static final Object lock = new Object();

    private static void ensureClientInitialized() {
        if (httpClient == null || !httpClient.isRunning()) {
            synchronized (lock) {
                if (httpClient == null || !httpClient.isRunning()) {
                    try {
                        if (httpClient != null) {
                            try {
                                httpClient.stop();
                            } catch (Exception e) {
                                logger.warn("Error stopping old HTTP client");
                                logger.debug(ExceptionUtils.getStackTrace(e), e);
                            }
                        }
                        httpClient = new HttpClient(new SslContextFactory.Client());
                        httpClient.setMaxRequestsQueuedPerDestination(MAX_RESULT_QUEUED_PER_DESTINATION);
                        httpClient.setConnectTimeout(CONNECTION_TIMEOUT);
                        httpClient.setIdleTimeout(REQUEST_TIMEOUT);
                        httpClient.setCookieStore(new HttpCookieStore.Empty());
                        httpClient.start();
                    } catch (Exception e) {
                        logger.error("Failed to initialize HTTP client");
                        logger.debug(ExceptionUtils.getStackTrace(e), e);
                        throw new RuntimeException("Failed to initialize HTTP client", e);
                    }
                }
            }
        }
    }

    public static String fetchContent(String url) {
        String content = null;
        boolean fetched = false;
        Exception lastException = null;
        int attempts = 0;
        long sleepTimeMs = RETRY_DELAY_MS;
        do {
            attempts++;
            try {
                content = HttpClientUtil.getContent(url.toString());
                fetched = true;
            } catch (Exception e) {
                lastException = e;
                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException e1) {
                    logger.error("Thread interrupted while waiting for sub-thread to finish", e1);
                    Thread.currentThread().interrupt();
                }
                if (sleepTimeMs < MAX_RETRY_DELAY_MS) {
                    sleepTimeMs += RETRY_DELAY_MS;
                }
            }
        } while (!fetched && attempts < MAX_RETRIES);
        if (!fetched) {
            logger.error("Failed to fetch content from URL: {} after {} attempts", url, attempts);
            logger.debug(ExceptionUtils.getStackTrace(lastException), lastException);
            throw lastException;
        }
        return content;
    }

    static String getContent(String url) throws Exception {
        ensureClientInitialized();
        try {
            ContentResponse response = httpClient.newRequest(url)
            .timeout(CONNECTION_TIMEOUT + REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
            .send();
            if (response.getStatus() >= 400) {
                throw new RuntimeException("HTTP error " + response.getStatus() + " for URL: " + url);
            }
            return response.getContentAsString();
        } catch (RejectedExecutionException e) {
            throw new RuntimeException("Request rejected for URL: " + url, e);
        } catch (Exception e) {
            throw e;
        }
    }

    public static void shutdown() {
        synchronized (lock) {
            try {
                if (httpClient != null) {
                    httpClient.stop();
                }
            } catch (Exception e) {
                logger.error("Failed to shutdown HTTP client");
                logger.debug(ExceptionUtils.getStackTrace(e), e);
            }
        }
    }
}