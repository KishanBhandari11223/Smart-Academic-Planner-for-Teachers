package com.adaptiveplanner.planner.auth;

public class InvalidOtpRequestException extends RuntimeException {
    public InvalidOtpRequestException(String message) {
        super(message);
    }
}
