package com.hospital.appointment.exception;

/**
 * Custom Exception thrown when an entity is not found in the repository.
 * Triggers an HTTP 404 Not Found response in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
