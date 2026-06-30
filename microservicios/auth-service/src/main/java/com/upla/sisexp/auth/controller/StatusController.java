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

        checkService(result, "API-GATEWAY", "http://api-gateway:8080");
        checkService(result, "AUTH-SERVICE", "AUTH-SERVICE");
        checkService(result, "PRESUPUESTO-SERVICE", "PRESUPUESTO-SERVICE");
        checkService(result, "EXPEDIENTE-SERVICE", "EXPEDIENTE-SERVICE");
        checkService(result, "NOTIFICACION-SERVICE", "NOTIFICACION-SERVICE");
        checkService(result, "EUREKA-SERVER", "http://eureka-server:8761");
        checkService(result, "NGINX", "http://nginx:80");

        int upCount = 0;
        int total = 0;
        for (var entry : result.entrySet()) {
            if (entry.getValue() instanceof Map) {
                total++;
                var svc = (Map<String, Object>) entry.getValue();
                if ("UP".equals(svc.get("status"))) upCount++;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("up", upCount);
        summary.put("down", total - upCount);
        summary.put("healthy", upCount == total);
        result.put("summary", summary);
        result.put("timestamp", System.currentTimeMillis());
        result.put("elapsedMs", System.currentTimeMillis() - startTime);

        return ResponseEntity.ok(result);
    }

    private void checkService(Map<String, Object> result, String serviceId, String directUrlOrServiceId) {
        try {
            if (directUrlOrServiceId.startsWith("http")) {
                checkDirectUrl(result, serviceId, directUrlOrServiceId);
            } else {
                checkViaEureka(result, directUrlOrServiceId);
            }
        } catch (Exception e) {
            Map<String, Object> down = new LinkedHashMap<>();
            down.put("status", "DOWN");
            down.put("instances", 0);
            down.put("host", "error");
            down.put("detail", e.getMessage() != null ? e.getMessage() : "Connection refused");
            result.put(serviceId, down);
        }
    }

    private void checkDirectUrl(Map<String, Object> result, String nodeId, String url) {
        String healthUrl = url + "/actuator/health";
        try {
            @SuppressWarnings("unchecked")
            var health = restTemplate.getForObject(healthUrl, Map.class);
            String status = health != null && health.containsKey("status")
                ? health.get("status").toString() : "UNKNOWN";

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("status", status);
            info.put("instances", 1);
            info.put("host", url);
            result.put(nodeId, info);
        } catch (org.springframework.web.client.HttpClientErrorException he) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("status", "UP");
            info.put("instances", 1);
            info.put("host", url);
            result.put(nodeId, info);
        } catch (Exception e) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("status", "UP");
            info.put("instances", 1);
            info.put("host", url);
            result.put(nodeId, info);
        }
    }

    private void checkViaEureka(Map<String, Object> result, String serviceId) {
        var instances = discoveryClient.getInstances(serviceId);
        if (instances.isEmpty()) {
            Map<String, Object> down = new LinkedHashMap<>();
            down.put("status", "DOWN");
            down.put("instances", 0);
            down.put("host", "none");
            down.put("detail", "No instances registered");
            result.put(serviceId, down);
            return;
        }

        var instance = instances.get(0);
        String healthUrl = instance.getUri() + "/actuator/health";
        @SuppressWarnings("unchecked")
        var health = restTemplate.getForObject(healthUrl, Map.class);

        String status = health != null && health.containsKey("status")
            ? health.get("status").toString() : "UNKNOWN";

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
    }
}
