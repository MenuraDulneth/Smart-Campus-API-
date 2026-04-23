package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps SensorUnavailableException to HTTP 403 Forbidden.
 *
 * 403 is semantically correct here: the client is authenticated and the resource
 * exists, but the current operational state of the sensor (MAINTENANCE) prohibits
 * the action. The server understood the request but refuses to fulfil it.
 */
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 403);
        error.put("error", "Forbidden");
        error.put("code", "SENSOR_UNAVAILABLE");
        error.put("message", exception.getMessage());
        error.put("hint", "Change the sensor status to 'ACTIVE' before posting new readings.");
        error.put("timestamp", System.currentTimeMillis());

        return Response
                .status(Response.Status.FORBIDDEN)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
