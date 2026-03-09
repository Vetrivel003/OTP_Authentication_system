package com.project.spring.core.exception;

public class OtpException extends RuntimeException {

    private final String errorCode;

    public OtpException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
