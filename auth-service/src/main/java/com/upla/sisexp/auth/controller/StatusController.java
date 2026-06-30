package com.upla.sisexp.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class StatusController {

    @Autowired
    private DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();

        List<String> services = List.of(
            "AUTH-SERVICE",
            "PRESUPUESTO-SERVICE",
            "EXPEDIENTE-SERVICE",
            "NOTIFICACION-SERVICE"
        );

        int upCount = 0;
        int totalServices = services.size();

        for (String serviceId : services) {
            try {
                var instances = discoveryClient.getInstances(serviceId);
                if (instances.isEmpty()) {
                    Map<String, Object> down = new LinkedHashMap<>();
                    down.put("status", "DOWN");
                    down.put("instances", 0);
                    down.put("host", "none");
                    down.put("detail", "No instances registered");
                    result.put(serviceId, down);
                    continue;
                }

                var instance = instances.get(0);
                String healthUrl = instance.getUri() + "/actuator/health";
                @SuppressWarnings("unchecked")
                var health = restTemplate.getForObject(healthUrl, Map.class);

                String status = health != null && health.containsKey("status")
                    ? health.get("status").toString()
                    : "UNKNOWN";

                if ("UP".equals(status)) upCount++;

                Map<String, Object> serviceInfo = new LinkedHashMap<>();
                serviceInfo.put("status", status);
                serviceInfo.put("instances", instances.size());
                serviceInfo.put("host", instance.getHost());
                serviceInfo.put("port", instance.getPort());

                if (health != null && health.containsKey("components")) {
                    @SuppressWarnings("unchecked")
                    var components = (Map<String, Object>) health.get("components");
                    Map<String, String> componentStatus = new LinkedHashMap<>();
                    for (var entry : components.entrySet()) {
                        @SuppressWarnings("unchecked")
                        var comp = (Map<String, Object>) entry.getValue();
                        componentStatus.put(entry.getKey(),
                            comp.getOrDefault("status", "UNKNOWN").toString());
                    }
                    serviceInfo.put("components", componentStatus);
                }

                result.put(serviceId, serviceInfo);

            } catch (Exception e) {
                Map<String, Object> down = new LinkedHashMap<>();
                down.put("status", "DOWN");
                down.put("instances", 0);
                down.put("host", "error");
                down.put("detail", e.getMessage() != null ? e.getMessage() : "Connection refused");
                result.put(serviceId, down);
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", totalServices);
        summary.put("up", upCount);
        summary.put("down", totalServices - upCount);
        summary.put("healthy", upCount == totalServices);
        result.put("summary", summary);

        result.put("timestamp", System.currentTimeMillis());
        long elapsed = System.currentTimeMillis() - startTime;
        result.put("elapsedMs", elapsed);

        return ResponseEntity.ok(result);
    }
}
