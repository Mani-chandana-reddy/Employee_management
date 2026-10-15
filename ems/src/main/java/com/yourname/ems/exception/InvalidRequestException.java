package com.yourname.ems.exception;

/** Business-rule / bad-parameter violation that maps to HTTP 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
