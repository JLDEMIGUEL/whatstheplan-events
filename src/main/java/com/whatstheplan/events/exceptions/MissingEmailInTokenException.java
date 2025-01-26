package com.whatstheplan.events.exceptions;

public class MissingEmailInTokenException extends RuntimeException {
    public MissingEmailInTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
