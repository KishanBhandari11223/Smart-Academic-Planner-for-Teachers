package com.adaptiveplanner.planner.auth;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class EmailRequestLock {
    private final ReentrantLock[] stripes = new ReentrantLock[256];

    public EmailRequestLock() {
        for (int index = 0; index < stripes.length; index++) {
            stripes[index] = new ReentrantLock();
        }
    }

    public <T> T withEmailLock(String email, Supplier<T> action) {
        ReentrantLock lock = stripes[Math.floorMod(email.hashCode(), stripes.length)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
