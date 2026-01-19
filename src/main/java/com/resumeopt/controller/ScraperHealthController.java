package com.resumeopt.controller;

import com.resumeopt.service.ScraperHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for scraper health monitoring
 */
@RestController
@RequestMapping("/api/scraper")
public class ScraperHealthController {

    @Autowired
    private ScraperHealthService scraperHealthService;

    /**
     * Get overall health status for all scrapers
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getOverallHealth() {
        Map<String, ScraperHealthService.ScraperHealth> allHealth = scraperHealthService.getAllHealth();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("portals", allHealth);
        response.put("totalPortals", allHealth.size());

        // Calculate overall statistics
        long healthyCount = allHealth.values().stream()
                .filter(h -> "HEALTHY".equals(h.getStatus()))
                .count();
        long degradedCount = allHealth.values().stream()
                .filter(h -> "DEGRADED".equals(h.getStatus()))
                .count();
        long failingCount = allHealth.values().stream()
                .filter(h -> "FAILING".equals(h.getStatus()))
                .count();

        response.put("healthyCount", healthyCount);
        response.put("degradedCount", degradedCount);
        response.put("failingCount", failingCount);

        return ResponseEntity.ok(response);
    }

    /**
     * Get health status for a specific portal
     */
    @GetMapping("/health/{portalName}")
    public ResponseEntity<Map<String, Object>> getPortalHealth(@PathVariable String portalName) {
        ScraperHealthService.ScraperHealth health = scraperHealthService.getHealth(portalName);

        Map<String, Object> response = new HashMap<>();
        if (health != null) {
            response.put("success", true);
            response.put("portal", health);
        } else {
            response.put("success", false);
            response.put("error", "Portal not found: " + portalName);
        }

        return ResponseEntity.ok(response);
    }
}
