package com.blur.backend;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class JsonTransformerTest {
    private JsonTransformer transformer;
    private JsonTransformerExpose transformerExpose;
    private Search search;

    private static final String TEST_URL = "https://example.com";

    @BeforeEach
    void setUp() {
        transformer = new JsonTransformer();
        transformerExpose = new JsonTransformerExpose();
        Term term = new Term("test");
        search = new Search(term, TEST_URL);
    }

    @Test
    void testRegularTransformer() {
        String json = transformer.render(search);
        assertNotNull(json);
        assertTrue(json.contains("id")); // ID should be included
        assertTrue(json.contains("created")); // Status should be included
        assertTrue(json.contains("test")); // Keyword should be included in regular version
        assertTrue(json.contains(TEST_URL)); // Base URL should be included in regular version
    }

    @Test
    void testExposeTransformer() {
        String json = transformerExpose.render(search);
        assertNotNull(json);
        assertTrue(json.contains("id")); // ID should be included
        assertFalse(json.contains("test")); // Keyword should not be included in expose version
        assertFalse(json.contains("created")); // Status should not be included
    }

    @Test
    void testNullInput() {
        String json = transformer.render(null);
        assertNull(json);
    }
}