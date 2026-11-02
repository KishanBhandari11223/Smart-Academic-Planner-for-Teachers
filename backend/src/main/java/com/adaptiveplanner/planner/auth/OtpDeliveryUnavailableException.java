package com.adaptiveplanner.planner.auth;

public class OtpDeliveryUnavailableException extends RuntimeException {
    public OtpDeliveryUnavailableException() {
        super("Email delivery is not configured.");
    }
}
