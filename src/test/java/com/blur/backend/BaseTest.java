package com.blur.backend;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import spark.Service;

public class BaseTest {
    protected static final int TEST_PORT = 4567;
    protected static Service sparkService;

    @BeforeAll
    static void initSpark() {
        // Create a new Spark service instance
        sparkService = Service
            .ignite()
            .port(TEST_PORT);
        sparkService.init();
        sparkService.awaitInitialization();
    }

    @AfterAll
    static void stopSpark() {
        if (sparkService != null) {
            sparkService.stop();
            sparkService.awaitStop();
        }
    }
}