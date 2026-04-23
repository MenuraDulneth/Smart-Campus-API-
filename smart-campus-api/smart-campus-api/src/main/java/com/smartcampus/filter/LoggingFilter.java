package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * API Observability Logging Filter
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter to intercept
 * every HTTP request entering and every HTTP response leaving the API.
 *
 * WHY USE FILTERS FOR CROSS-CUTTING CONCERNS?
 *
 * Cross-cutting concerns like logging, authentication, CORS, and rate-limiting affect
 * ALL endpoints uniformly. If we inserted Logger.info() statements manually into every
 * resource method, we would:
 *
 *   1. VIOLATE DRY (Don't Repeat Yourself): The same logging code would be duplicated
 *      across dozens of methods, making it error-prone to maintain.
 *
 *   2. COUPLE CONCERNS: Business logic (e.g., deleting a room) would be mixed with
 *      infrastructure logic (logging), violating the Single Responsibility Principle.
 *
 *   3. RISK OMISSION: A developer adding a new endpoint might forget to add the logging
 *      statement. Filters guarantee 100% coverage automatically.
 *
 *   4. HARDER TO CHANGE: Swapping java.util.logging for SLF4J would require modifying
 *      every resource class. With a filter, you change one file.
 *
 * The @Provider annotation registers this filter with the JAX-RS runtime automatically,
 * so it applies globally to every request/response without any further configuration.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Intercepts every incoming HTTP request BEFORE it reaches the resource method.
     * Logs the HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info(String.format("[REQUEST]  --> %s %s", method, uri));
    }

    /**
     * Intercepts every outgoing HTTP response AFTER the resource method has completed.
     * Logs the HTTP method, URI, and final response status code.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String method     = requestContext.getMethod();
        String uri        = requestContext.getUriInfo().getRequestUri().toString();
        int    statusCode = responseContext.getStatus();
        LOGGER.info(String.format("[RESPONSE] <-- %s %s | Status: %d", method, uri, statusCode));
    }
}
