package com.upla.sisexp.gateway;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ActivityBuffer {
    private static final int CAPACITY = 200;
    private final ActivityEvent[] buffer = new ActivityEvent[CAPACITY];
    private final AtomicInteger cursor = new AtomicInteger(0);

    public void add(ActivityEvent event) {
        int idx = cursor.getAndIncrement() % CAPACITY;
        buffer[idx] = event;
    }

    public List<ActivityEvent> getRecent(long minutes) {
        Instant since = Instant.now().minusSeconds(minutes * 60);
        List<ActivityEvent> result = new ArrayList<>();
        int current = cursor.get();
        for (int i = 0; i < CAPACITY; i++) {
            int idx = (current - 1 - i + CAPACITY) % CAPACITY;
            ActivityEvent e = buffer[idx];
            if (e == null) break;
            if (e.getTimestamp().isAfter(since)) {
                result.add(e);
            }
        }
        return result;
    }

    public List<ActivityEvent> getByService(String service, long minutes) {
        Instant since = Instant.now().minusSeconds(minutes * 60);
        List<ActivityEvent> result = new ArrayList<>();
        int current = cursor.get();
        for (int i = 0; i < CAPACITY; i++) {
            int idx = (current - 1 - i + CAPACITY) % CAPACITY;
            ActivityEvent e = buffer[idx];
            if (e == null) break;
            if (e.getService().equalsIgnoreCase(service) && e.getTimestamp().isAfter(since)) {
                result.add(e);
            }
        }
        return result;
    }
}
