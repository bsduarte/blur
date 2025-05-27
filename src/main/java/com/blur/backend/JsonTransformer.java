package com.blur.backend;

import com.google.gson.Gson;

import spark.ResponseTransformer;


public class JsonTransformer implements ResponseTransformer {

    private final Gson gson = new Gson();

    @Override
    public String render(Object model) {
        return model != null ? gson.toJson(model) : null;
    }
}
