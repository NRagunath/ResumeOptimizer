package com.resumeopt.service;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class ScrapingHealthIndicator implements HealthIndicator {

    private static final String TEST_URL = "https://www.google.com"; // Reliable external target

    @Override
    public Health health() {
        try {
            if (checkInternetConnectivity()) {
                return Health.up()
                        .withDetail("connectivity", "Online")
                        .withDetail("target", TEST_URL)
                        .build();
            } else {
                return Health.down()
                        .withDetail("connectivity", "Offline")
                        .withDetail("error", "Could not reach external network")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("connectivity", "Error")
                    .build();
        }
    }

    private boolean checkInternetConnectivity() {
        try {
            URL url = new URL(TEST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException e) {
            return false;
        }
    }
}
