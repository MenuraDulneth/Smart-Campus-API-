package com.smartcampus.application;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Main entry point for the Smart Campus API.
 * Starts an embedded Grizzly HTTP server on port 8080.
 *
 * Usage: java -jar smart-campus-api-1.0.0.jar
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static HttpServer startServer() {
        ResourceConfig rc = ResourceConfig.forApplication(new SmartCampusApplication());
        rc.register(JacksonFeature.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
        
    }


    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        LOGGER.info("Smart Campus API started at: " + BASE_URI);
        LOGGER.info("Discovery endpoint: " + BASE_URI);
        LOGGER.info("Press ENTER to stop the server...");
        System.in.read();
        server.shutdownNow();
    }
}
