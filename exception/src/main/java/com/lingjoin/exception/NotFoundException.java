package com.lingjoin.exception;

import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The type Not found exception.
 */
public class NotFoundException extends ResponseStatusException {
    /**
     * Instantiates a new Not found exception.
     *
     * @param reason the reason
     * @param cause  the cause
     */
    public NotFoundException(String reason, Throwable cause) {
        super(NOT_FOUND, reason, cause);
    }
}
