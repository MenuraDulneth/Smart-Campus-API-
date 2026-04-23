package com.smartcampus.exception;

/**
 * Thrown when a POST reading is attempted on a Sensor whose status is "MAINTENANCE".
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
