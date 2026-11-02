package com.adaptiveplanner.planner.auth;

public class OtpRateLimitException extends RuntimeException {
    public OtpRateLimitException() {
        super("Too many requests. Please wait before trying again.");
    }
}
