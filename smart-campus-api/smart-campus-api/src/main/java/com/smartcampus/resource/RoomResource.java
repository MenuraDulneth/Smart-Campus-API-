package com.smartcampus.resource;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

/**
 * Room Resource
 *
 * Manages the /api/v1/rooms collection and individual rooms at /api/v1/rooms/{roomId}.
 *
 * Endpoints:
 *   GET    /api/v1/rooms            - List all rooms
 *   POST   /api/v1/rooms            - Create a new room
 *   GET    /api/v1/rooms/{roomId}   - Get a specific room by ID
 *   DELETE /api/v1/rooms/{roomId}   - Delete a room (blocked if sensors are assigned)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/rooms
     * Returns a complete list of all registered rooms.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> allRooms = store.getRooms().values();
        List<Room> roomList = new ArrayList<>(allRooms);
        return Response.ok(roomList).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Validates that id and name are provided.
     * Returns 201 Created with a Location header pointing to the new resource.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            Map<String, Object> error = buildError(400, "Bad Request", "Room 'id' is required.");
            return Response.status(400).entity(error).build();
        }
        if (room.getName() == null || room.getName().isBlank()) {
            Map<String, Object> error = buildError(400, "Bad Request", "Room 'name' is required.");
            return Response.status(400).entity(error).build();
        }
        if (store.roomExists(room.getId())) {
            Map<String, Object> error = buildError(409, "Conflict", "A room with ID '" + room.getId() + "' already exists.");
            return Response.status(409).entity(error).build();
        }

        // Ensure sensorIds list is initialised
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.putRoom(room);

        URI location = URI.create("/api/v1/rooms/" + room.getId());
        return Response.created(location).entity(room).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns detailed metadata for a single room.
     * Returns 404 if the room does not exist.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            Map<String, Object> error = buildError(404, "Not Found", "Room with ID '" + roomId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     *
     * Decommissions a room. Protected by business logic:
     * - If the room does not exist → 404 Not Found
     * - If the room has sensors assigned → throws RoomNotEmptyException → 409 Conflict
     * - If deletion is successful → 200 OK with confirmation message
     *
     * IDEMPOTENCY NOTE:
     * The first DELETE call succeeds (200). Subsequent identical DELETE calls receive 404,
     * because the room no longer exists in the system. The server state after the first
     * call and all subsequent calls is identical (room absent), which satisfies the
     * RFC definition of idempotency — repeated calls produce the same final state.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            Map<String, Object> error = buildError(404, "Not Found", "Room with ID '" + roomId + "' does not exist.");
            return Response.status(404).entity(error).build();
        }

        // Business rule: cannot delete a room that has active sensors
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Room '" + roomId + "' cannot be deleted because it has " +
                room.getSensorIds().size() + " sensor(s) still assigned: " +
                room.getSensorIds()
            );
        }

        store.deleteRoom(roomId);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Room '" + roomId + "' has been successfully decommissioned.");
        result.put("deletedRoomId", roomId);
        return Response.ok(result).build();
    }

    // Utility: build a consistent error body
    private Map<String, Object> buildError(int status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
