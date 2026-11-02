package com.adaptiveplanner.planner.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class RequestRateLimiter {
    private final Map<String, ArrayDeque<Instant>> requests = new HashMap<>();

    public synchronized void check(String action, String clientAddress, int maximum, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        String key = action + ":" + (clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress);
        ArrayDeque<Instant> timestamps = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.removeFirst();
        }
        if (timestamps.size() >= maximum) {
            throw new OtpRateLimitException();
        }
        timestamps.addLast(now);
        if (requests.size() > 10_000) {
            Iterator<Map.Entry<String, ArrayDeque<Instant>>> iterator = requests.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ArrayDeque<Instant>> entry = iterator.next();
                if (entry.getValue().isEmpty() || entry.getValue().peekLast().isBefore(cutoff)) {
                    iterator.remove();
                }
            }
        }
    }
}
