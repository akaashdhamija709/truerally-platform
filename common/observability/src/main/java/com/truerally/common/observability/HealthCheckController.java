package com.truerally.common.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping("/healthz")
    public Map<String, String> healthCheck() {
        return Map.of("status", "UP");
    }
}
