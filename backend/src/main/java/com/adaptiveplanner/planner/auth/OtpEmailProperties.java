package com.adaptiveplanner.planner.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public class OtpEmailProperties {
    private boolean enabled;
    private String from;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
}
