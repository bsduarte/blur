package com.blur.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TermDeserializerTest {
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(Term.class, new TermDeserializer())
        .create();

    @Test
    void testTermInitialization() {
        String searchJson = "{\"keyword\":\"test\"}";
        Term term = gson.fromJson(searchJson, Term.class);
        assertEquals("test", term.getKeyword());
    }
}
