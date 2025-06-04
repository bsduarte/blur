package com.blur.backend;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class TermDeserializer implements JsonDeserializer<Term> {
    @Override
    public Term deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonKeyword = json.getAsJsonObject();
        if (!jsonKeyword.has("keyword")) {
            throw new JsonParseException("Missing 'keyword' field in Term JSON");
        }
        String keyword = jsonKeyword.get("keyword").getAsString(); 
        return new Term(keyword);
    }
}
