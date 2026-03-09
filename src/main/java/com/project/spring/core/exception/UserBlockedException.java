package com.project.spring.core.exception;

public class UserBlockedException extends RuntimeException {
    public UserBlockedException(String reason) {
        super("User is blocked: " + reason);
    }
}
