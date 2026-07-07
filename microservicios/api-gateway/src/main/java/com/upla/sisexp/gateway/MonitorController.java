package com.upla.sisexp.gateway;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final ActivityBuffer buffer;

    public MonitorController(ActivityBuffer buffer) {
        this.buffer = buffer;
    }

    @GetMapping("/activity")
    public List<ActivityEvent> getActivity(@RequestParam(defaultValue = "5") long since) {
        return buffer.getRecent(since);
    }

    @GetMapping("/activity/service")
    public List<ActivityEvent> getByService(
            @RequestParam String name,
            @RequestParam(defaultValue = "5") long since) {
        return buffer.getByService(name, since);
    }
}
