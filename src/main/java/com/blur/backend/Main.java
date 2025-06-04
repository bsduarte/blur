package com.blur.backend;

import static spark.Spark.*;

import java.util.concurrent.ConcurrentHashMap;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.net.URISyntaxException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    private static final JsonTransformer jsonTransformer = new JsonTransformer();
    private static final JsonTransformerExpose jsonTransformerExpose = new JsonTransformerExpose();
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(Term.class, new TermDeserializer())
        .create();
    private static final ConcurrentHashMap<String, Search> searches = new ConcurrentHashMap<>();

    private static URL baseUrl;
    private static boolean isTestMode = false;

    public static void setTestMode(boolean testMode) {
        isTestMode = testMode;
    }

    public static void main(String[] args) {
        try {
            initialize();
            setupRoutes();
            setupShutdownHook();
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            logger.debug(ExceptionUtils.getStackTrace(e), e);
            if (!isTestMode) {
                System.exit(1);
            }
        }
    }

    private static void initialize() {
        try {
            baseUrl = new URI(System.getProperty("BASE_URL", System.getenv("BASE_URL"))).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalStateException("Invalid BASE_URL.");
        }
    }

    private static void setupRoutes() {
        before((request, response) -> {
            response.type("application/json");
        });

        get("/crawl/:id", (req, res) -> {
            String id = req.params("id");
            Search search = searches.get(id);
            if (search == null) {
                halt(404, "Search " + id + " not found");
            }
            return search;
        }, jsonTransformer);

        post("/crawl", (req, res) -> {
            final Term term;
            try {
                term = gson.fromJson(req.body(), Term.class);
            } catch (Exception e) {
                halt(400, "Invalid request body: " + e.getMessage());
                return null;
            }
            Search search = new Search(term, baseUrl);
            searches.put(search.getId(), search);
            search.start();
            
            return search;
        }, jsonTransformerExpose);
    }    

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            HttpClientUtil.shutdown();
        }));
    }
}
