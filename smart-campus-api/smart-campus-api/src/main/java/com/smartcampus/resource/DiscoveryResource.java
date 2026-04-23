package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.QueryParam;

/**
 * Discovery / Root Endpoint
 *
 * GET /api/v1
 *
 * Returns API metadata including version info, admin contact, and resource map.
 * This implements the HATEOAS principle by providing navigable links to all
 * primary resource collections, enabling clients to explore the API without
 * relying solely on static external documentation.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {



    @GET
    public Response discover(@QueryParam("crash") String crash) {
        if ("true".equalsIgnoreCase(crash)) {
            throw new RuntimeException("Intentional 500 test");
        }
        Map<String, Object> response = new HashMap<>();

        // Versioning info
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("status", "operational");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");

        // Administrative contact
        Map<String, String> contact = new HashMap<>();
        contact.put("team", "Campus Infrastructure Team");
        contact.put("email", "smartcampus@westminster.ac.uk");
        contact.put("institution", "University of Westminster");
        response.put("contact", contact);

        // HATEOAS: Resource map — links to all primary collections
        Map<String, String> links = new HashMap<>();
        links.put("self",     "/api/v1");
        links.put("rooms",    "/api/v1/rooms");
        links.put("sensors",  "/api/v1/sensors");
        response.put("_links", links);

        // Resource descriptions
        Map<String, String> resources = new HashMap<>();
        resources.put("rooms",   "Campus room registry — create, read, and delete physical spaces");
        resources.put("sensors", "IoT sensor registry — manage sensors and their historical readings");
        response.put("resources", resources);

        return Response.ok(response).build();
    }
}
