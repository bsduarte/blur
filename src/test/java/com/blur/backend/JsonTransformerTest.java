package com.blur.backend;

import org.junit.jupiter.api.Test;

import com.blur.backend.JsonTransformer;
import com.blur.backend.JsonTransformerExpose;
import com.blur.backend.Search;
import com.blur.backend.Term;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class JsonTransformerTest {
    private JsonTransformer transformer;
    private JsonTransformerExpose transformerExpose;
    private Search search;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonTransformer();
        transformerExpose = new JsonTransformerExpose();
        Term term = new Term("test");
        search = new Search("id123", term);
    }

    @Test
    void testRegularTransformer() {
        String json = transformer.render(search);
        assertNotNull(json);
        assertTrue(json.contains("id123")); // ID should be included
        assertTrue(json.contains("active")); // Status should be included
        assertFalse(json.contains("test")); // Keyword should not be included in regular version
    }

    @Test
    void testExposeTransformer() {
        String json = transformerExpose.render(search);
        assertNotNull(json);
        assertTrue(json.contains("id123")); // ID should be included
        assertFalse(json.contains("test")); // Keyword should not be included in expose version
        assertFalse(json.contains("active")); // Status should not be included
    }

    @Test
    void testNullInput() {
        String json = transformer.render(null);
        assertNull(json);
    }
}