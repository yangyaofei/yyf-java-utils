package com.lingjoin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The type Conflict exception.
 */
@SuppressWarnings("unused")
public class ConflictException extends ResponseStatusException {
    /**
     * Instantiates a new Conflict exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, message, cause);
    }

    /**
     * Instantiates a new Conflict exception.
     *
     * @param message the message
     */
    public ConflictException(String message) {
        this(message, null);
    }

    /**
     * Instantiates a new Conflict exception.
     */
    public ConflictException() {
        this("", null);
    }
}
