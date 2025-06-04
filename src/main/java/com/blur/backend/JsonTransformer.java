package com.blur.backend;

import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.ResponseTransformer;


public class JsonTransformer implements ResponseTransformer {
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .create();

    @Override
    public String render(Object model) {
        return model != null ? gson.toJson(model) : null;
    }
}
