package com.blur.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.ResponseTransformer;

public class JsonTransformerExpose implements ResponseTransformer {
    private final Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    @Override
    public String render(Object model) {
        return model != null ? gson.toJson(model) : null;
    }
}
