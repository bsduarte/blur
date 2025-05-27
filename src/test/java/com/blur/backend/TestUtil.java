package com.blur.backend;

import spark.Route;
import spark.Spark;

public class TestUtil {
    private static final String SPARK_URL = "http://localhost:";
    
    private static synchronized void initializeServer() {
        Spark.stop();
        Spark.awaitStop();
        Spark.port(0);
    }

    public static String serverAddr() {
        return SPARK_URL + Spark.port();
    }

    public static synchronized String createTestServer(String responseContent) {
        initializeServer();
        Spark.get("/test/*", (req, res) -> responseContent);
        Spark.awaitInitialization();
        return serverAddr() + "/test/";
    }

    public static synchronized String createTestServer(Route handler) {
        initializeServer();
        Spark.get("/test/*", handler);
        Spark.post("/test/*", handler);
        Spark.awaitInitialization();
        return serverAddr() + "/test/";
    }

    public static String createTestServerWithDelay(String responseContent, long delayMs) {
        return createTestServer((req, res) -> {
            Thread.sleep(delayMs);
            return responseContent;
        });
    }

    public static synchronized String createTestServerWithFailures(String responseContent, int failureCount, int statusCode) {
        initializeServer();
        Spark.get("/test/*", new RetryTestRoute(responseContent, failureCount, statusCode));
        Spark.awaitInitialization();
        return serverAddr() + "/test/";
    }

    public static void stopTestServer() {
        spark.Spark.stop();
        spark.Spark.awaitStop();
    }

    private static class RetryTestRoute implements Route {
        private final String successContent;
        private int remainingFailures;
        private final int failureStatus;

        public RetryTestRoute(String successContent, int failureCount, int failureStatus) {
            this.successContent = successContent;
            this.remainingFailures = failureCount;
            this.failureStatus = failureStatus;
        }

        @Override
        public Object handle(spark.Request request, spark.Response response) {
            if (remainingFailures > 0) {
                remainingFailures--;
                response.status(failureStatus);
                return "Error";
            }
            return successContent;
        }
    }
}