package com.smartcampus.data;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized, thread-safe in-memory data store for the Smart Campus API.
 *
 * SINGLETON PATTERN:
 * Because JAX-RS resource classes are request-scoped by default (a new instance per request),
 * they cannot hold state themselves. This singleton DataStore is the solution: it lives for
 * the entire JVM lifetime and is shared across all resource instances.
 *
 * THREAD SAFETY:
 * ConcurrentHashMap is used for all data structures to prevent race conditions when multiple
 * requests read and write concurrently. This ensures no data loss or corruption under load.
 *
 * SAMPLE DATA:
 * Pre-populated with representative data so the API is immediately testable without setup.
 */
public class DataStore {

    // --- Singleton ---
    private static final DataStore INSTANCE = new DataStore();

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // --- Data Structures ---
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    // Key: sensorId -> list of readings
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // Private constructor with sample seed data
    private DataStore() {
        // Seed Rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        Room r3 = new Room("HALL-A", "Main Lecture Hall A", 200);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        // Seed Sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LAB-101");
        Sensor s4 = new Sensor("TEMP-002", "Temperature", "OFFLINE", 18.0, "HALL-A");

        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);
        sensors.put(s4.getId(), s4);

        // Link sensor IDs to their rooms
        r1.addSensorId("TEMP-001");
        r1.addSensorId("CO2-001");
        r2.addSensorId("OCC-001");
        r3.addSensorId("TEMP-002");

        // Seed some initial readings
        List<SensorReading> readings1 = new ArrayList<>();
        readings1.add(new SensorReading(21.0));
        readings1.add(new SensorReading(22.5));
        sensorReadings.put("TEMP-001", readings1);

        List<SensorReading> readings2 = new ArrayList<>();
        readings2.add(new SensorReading(410.0));
        sensorReadings.put("CO2-001", readings2);
    }

    // --- Room Operations ---
    public Map<String, Room> getRooms() { return rooms; }

    public Room getRoom(String id) { return rooms.get(id); }

    public void putRoom(Room room) { rooms.put(room.getId(), room); }

    public boolean deleteRoom(String id) {
        return rooms.remove(id) != null;
    }

    public boolean roomExists(String id) { return rooms.containsKey(id); }

    // --- Sensor Operations ---
    public Map<String, Sensor> getSensors() { return sensors; }

    public Sensor getSensor(String id) { return sensors.get(id); }

    public void putSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        sensorReadings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public boolean sensorExists(String id) { return sensors.containsKey(id); }

    // --- Sensor Reading Operations ---
    public List<SensorReading> getReadings(String sensorId) {
        return sensorReadings.getOrDefault(sensorId, new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}
