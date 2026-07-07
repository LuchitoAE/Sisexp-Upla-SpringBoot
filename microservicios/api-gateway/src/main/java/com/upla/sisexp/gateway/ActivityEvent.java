package com.upla.sisexp.gateway;

import java.time.Instant;

public class ActivityEvent {
    private final Instant timestamp;
    private final String service;
    private final String action;
    private final String description;
    private final String path;
    private final int status;
    private final String userEmail;

    public ActivityEvent(Instant timestamp, String service, String action, String description, String path, int status, String userEmail) {
        this.timestamp = timestamp;
        this.service = service;
        this.action = action;
        this.description = description;
        this.path = path;
        this.status = status;
        this.userEmail = userEmail;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getService() { return service; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
    public String getPath() { return path; }
    public int getStatus() { return status; }
    public String getUserEmail() { return userEmail; }
}
