package com.adaptiveplanner.planner.auth;

public interface OtpDeliveryService {
    void sendCode(String recipient, String code);
}
