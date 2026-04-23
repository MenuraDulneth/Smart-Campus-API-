package com.smartcampus.model;

import java.util.UUID;

/**
 * Represents a single historical reading captured by a sensor.
 * Each reading records a timestamp (epoch ms) and a measured value.
 */
public class SensorReading {

    private String id;
    private long timestamp;
    private double value;

    // Default constructor required for JSON deserialization
    public SensorReading() {}

    public SensorReading(double value) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value = value;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
