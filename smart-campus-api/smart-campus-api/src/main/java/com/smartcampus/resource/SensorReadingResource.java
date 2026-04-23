package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sub-Resource: Sensor Reading Resource
 *
 * Handles the /api/v1/sensors/{sensorId}/readings path segment.
 *
 * This class is NOT annotated with @Path at the class level because it is not
 * a root resource. It is instantiated by the SensorResource sub-resource locator,
 * which means JAX-RS injects the sensorId context from the parent path.
 *
 * Endpoints (relative to /api/v1/sensors/{sensorId}/readings):
 *   GET    /     - Retrieve full reading history for this sensor
 *   POST   /     - Append a new reading; updates parent Sensor's currentValue
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     *
     * Returns the complete historical log of readings for this sensor.
     * Readings are returned in chronological insertion order.
     */
    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getReadings(sensorId);

        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("count", readings.size());
        response.put("readings", readings);

        return Response.ok(response).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     *
     * Appends a new reading to this sensor's history.
     *
     * STATE CONSTRAINT:
     * If the sensor's status is "MAINTENANCE", it cannot physically accept new readings.
     * A SensorUnavailableException is thrown, which maps to 403 Forbidden.
     *
     * SIDE EFFECT:
     * Upon successful recording, the parent Sensor object's currentValue field is updated
     * to reflect the latest measurement, ensuring data consistency across the API.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        // Should not be null at this point (validated in locator), but guard defensively
        if (sensor == null) {
            Map<String, Object> error = buildError(404, "Not Found", "Sensor '" + sensorId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }

        // Business rule: sensors in MAINTENANCE cannot accept new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE and cannot record new readings. " +
                "Please restore the sensor to ACTIVE status before posting data."
            );
        }

        if (reading == null) {
            return Response.status(400).entity(buildError(400, "Bad Request", "Reading body is required.")).build();
        }

        // Assign a UUID and timestamp if not provided by the client
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading = new SensorReading(reading.getValue());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist reading
        store.addReading(sensorId, reading);

        // Side effect: update the parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Reading recorded successfully for sensor '" + sensorId + "'.");
        response.put("reading", reading);
        response.put("updatedSensorCurrentValue", sensor.getCurrentValue());

        URI location = URI.create("/api/v1/sensors/" + sensorId + "/readings");
        return Response.created(location).entity(response).build();
    }

    private Map<String, Object> buildError(int status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
