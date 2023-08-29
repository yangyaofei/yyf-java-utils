package io.github.yangyaofei.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The type Already exists exception.
 */
public class AlreadyExistsException extends ResponseStatusException {
    /**
     * Instantiates a new Already exists exception.
     */
    public AlreadyExistsException() {
        this(null, null);
    }

    /**
     * Instantiates a new Already exists exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public AlreadyExistsException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, message, cause);
    }
}
