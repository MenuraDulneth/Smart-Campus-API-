package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * This status is more semantically accurate than 404 Not Found when the problem
 * is a broken reference *inside* a valid, well-formed JSON payload:
 *
 * - 404 means "this URL/endpoint was not found" — the request path is the problem.
 * - 422 means "the request was understood, the JSON was valid, but the content
 *   is semantically unprocessable" — the referenced roomId field value is invalid.
 *
 * Using 422 gives client developers a precise, actionable signal: "your JSON was
 * parsed correctly, but one of your field values references something that does not exist."
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 422);
        error.put("error", "Unprocessable Entity");
        error.put("code", "LINKED_RESOURCE_NOT_FOUND");
        error.put("message", exception.getMessage());
        error.put("hint", "Ensure the 'roomId' you provided refers to an existing room in /api/v1/rooms.");
        error.put("timestamp", System.currentTimeMillis());

        return Response
                .status(422)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
