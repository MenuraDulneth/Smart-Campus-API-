package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global Safety Net Exception Mapper.
 *
 * Catches ANY Throwable not handled by a more specific mapper (e.g., NullPointerException,
 * IndexOutOfBoundsException, or any unanticipated runtime error).
 *
 * This ensures the API is "leak-proof": no raw Java stack trace or server error page
 * will ever be visible to an external client.
 *
 * SECURITY RATIONALE:
 * Exposing stack traces to external consumers is a significant security risk because:
 *
 * 1. INTERNAL PATHS: Stack frames reveal the exact package structure, class names, and
 *    method names of the application. This tells attackers which framework and version
 *    is in use and enables targeted exploitation of known CVEs.
 *
 * 2. LIBRARY VERSIONS: Exception messages from libraries (e.g., "Jersey 2.41") expose
 *    the exact dependency versions, making it trivial to look up public vulnerability
 *    databases (NVD, CVE) for matching exploits.
 *
 * 3. LOGIC FLAWS: Stack traces often reveal business logic details — for example, a
 *    NullPointerException on line 47 of SensorResource.java tells an attacker that a
 *    null check is missing there, and they can craft payloads to repeatedly trigger it.
 *
 * 4. DATABASE / INFRASTRUCTURE DETAILS: StackTraces from JDBC or ORM layers expose
 *    SQL queries, table names, and connection strings.
 *
 * The internal exception is logged server-side at SEVERE level for developers, but the
 * client receives only a generic, safe error response.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof javax.ws.rs.WebApplicationException) {
            return ((javax.ws.rs.WebApplicationException) exception).getResponse();
        }
        // Log the full stack trace server-side for debugging — NEVER send it to the client
        
        LOGGER.log(Level.SEVERE, "Unhandled exception intercepted by GlobalExceptionMapper: " + exception.getMessage(), exception);

        Map<String, Object> error = new HashMap<>();
        error.put("status", 500);
        error.put("error", "Internal Server Error");
        error.put("code", "UNEXPECTED_ERROR");
        error.put("message", "An unexpected error occurred on the server. Our team has been notified. Please try again later.");
        error.put("timestamp", System.currentTimeMillis());

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
