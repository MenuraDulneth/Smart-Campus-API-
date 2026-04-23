package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sensor Resource
 *
 * Manages the /api/v1/sensors collection.
 *
 * Endpoints:
 *   GET    /api/v1/sensors               - List all sensors (optional ?type= filter)
 *   POST   /api/v1/sensors               - Register a new sensor (validates roomId existence)
 *   GET    /api/v1/sensors/{sensorId}    - Get a specific sensor
 *   DELETE /api/v1/sensors/{sensorId}    - Remove a sensor
 *
 * Sub-resource locator:
 *   ANY    /api/v1/sensors/{sensorId}/readings  - Delegated to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     *
     * Returns all sensors. If the optional 'type' query parameter is provided,
     * only sensors matching that type (case-insensitive) are returned.
     *
     * Using @QueryParam is the preferred approach for collection filtering because:
     * - It is optional — clients that don't filter are not forced to provide a value
     * - It keeps the resource path clean (/sensors vs. /sensors/type/CO2)
     * - Multiple filters can be combined without path explosion (?type=CO2&status=ACTIVE)
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();

        List<Sensor> result;
        if (type != null && !type.isBlank()) {
            result = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        } else {
            result = new ArrayList<>(all);
        }

        return Response.ok(result).build();
    }

    /**
     * POST /api/v1/sensors
     *
     * Registers a new sensor. Before persisting, validates that:
     * 1. Required fields (id, type, status, roomId) are present
     * 2. The specified roomId actually refers to an existing room in the system
     *
     * If the roomId is invalid, throws LinkedResourceNotFoundException → 422 Unprocessable Entity.
     *
     * @Consumes(APPLICATION_JSON): If a client sends a different Content-Type (e.g., text/plain
     * or application/xml), JAX-RS will reject the request with HTTP 415 Unsupported Media Type
     * before this method is even invoked. The framework's content negotiation mechanism handles
     * the mismatch automatically.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(400).entity(buildError(400, "Bad Request", "Sensor 'id' is required.")).build();
        }
        if (sensor.getType() == null || sensor.getType().isBlank()) {
            return Response.status(400).entity(buildError(400, "Bad Request", "Sensor 'type' is required.")).build();
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            return Response.status(400).entity(buildError(400, "Bad Request", "Sensor 'status' is required.")).build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(400).entity(buildError(400, "Bad Request", "Sensor 'roomId' is required.")).build();
        }
        if (store.sensorExists(sensor.getId())) {
            return Response.status(409).entity(buildError(409, "Conflict", "Sensor '" + sensor.getId() + "' already exists.")).build();
        }

        // Validate that the referenced room actually exists
        Room room = store.getRoom(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: the referenced roomId '" + sensor.getRoomId() +
                "' does not exist in the system. Create the room first."
            );
        }

        // Persist the sensor and update the room's sensor list
        store.putSensor(sensor);
        room.addSensorId(sensor.getId());

        URI location = URI.create("/api/v1/sensors/" + sensor.getId());
        return Response.created(location).entity(sensor).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Returns a single sensor's details.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(404).entity(buildError(404, "Not Found", "Sensor '" + sensorId + "' does not exist.")).build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     * Removes a sensor and unlinks it from its parent room.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(404).entity(buildError(404, "Not Found", "Sensor '" + sensorId + "' does not exist.")).build();
        }

        // Unlink from parent room
        Room room = store.getRoom(sensor.getRoomId());
        if (room != null) {
            room.removeSensorId(sensorId);
        }

        store.getSensors().remove(sensorId);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Sensor '" + sensorId + "' has been removed.");
        result.put("deletedSensorId", sensorId);
        return Response.ok(result).build();
    }

    /**
     * Sub-Resource Locator: /api/v1/sensors/{sensorId}/readings
     *
     * Instead of defining all reading-related paths in this class, this method
     * delegates to a dedicated SensorReadingResource instance. JAX-RS will
     * forward any request matching {sensorId}/readings/** to that sub-resource class.
     *
     * This is the Sub-Resource Locator Pattern. It keeps SensorResource focused
     * only on sensor-level concerns, and moves reading concerns into their own class,
     * improving separation of concerns and maintainability in large APIs.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        // Validate sensor exists before delegating
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found.");
        }
        return new SensorReadingResource(sensorId);
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
