package com.smartcampus.exception;

/**
 * Thrown when a referenced resource (e.g., a roomId in a Sensor POST body)
 * does not exist in the system.
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
