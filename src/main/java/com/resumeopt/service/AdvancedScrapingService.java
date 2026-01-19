package com.resumeopt.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Advanced scraping service with rate limiting, caching, retry logic, and
 * intelligent error handling
 */
@Service
public class AdvancedScrapingService {

    // Rate limiting per domain
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

    // Cache for documents
    private final Map<String, CachedDocument> documentCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = TimeUnit.MINUTES.toMillis(15); // 15 minutes cache

    // Default delays with randomization to avoid detection
    private static final long DEFAULT_DELAY_MS = 5000; // Default 5 seconds
    private static final long MIN_DELAY_MS = 2000; // Minimum 2 seconds
    private static final long MAX_DELAY_MS = 8000; // Maximum 8 seconds
    private static final int MAX_RETRIES = 4; // Increased from 3 to 4
    private static final long INITIAL_RETRY_DELAY_MS = 3000; // Increased from 2s to 3s

    // User agents rotation
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Android 13; Mobile; rv:109.0) Gecko/115.0 Firefox/115.0",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0"
    };

    private int userAgentIndex = 0;

    /**
     * Fetch document with advanced features: rate limiting, caching, retries
     */
    public Document fetchDocument(String url) throws IOException {
        return fetchDocument(url, DEFAULT_DELAY_MS, MAX_RETRIES);
    }

    /**
     * Fetch document with custom delay and retries
     */
    public Document fetchDocument(String url, long delayMs, int maxRetries) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }

        // Check cache first
        CachedDocument cached = documentCache.get(url);
        if (cached != null && !cached.isExpired()) {
            return cached.document;
        }

        // Apply rate limiting
        String domain = extractDomain(url);
        applyRateLimit(domain, delayMs);

        // Retry logic with exponential backoff
        IOException lastException = null;
        long retryDelay = INITIAL_RETRY_DELAY_MS;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                Document doc = performRequest(url);

                // Cache successful response
                documentCache.put(url, new CachedDocument(doc, System.currentTimeMillis()));

                return doc;

            } catch (IOException e) {
                lastException = e;

                // Check if it's a rate limit error
                if (isRateLimitError(e)) {
                    if (attempt < maxRetries - 1) {
                        System.out.println("Rate limited for " + url + ", waiting " + retryDelay + "ms before retry "
                                + (attempt + 2));
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Request interrupted", ie);
                        }
                        retryDelay *= 2; // Exponential backoff
                        continue;
                    }
                }

                // For other errors, retry with backoff
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Request interrupted", ie);
                    }
                    retryDelay *= 2;
                }
            }
        }

        // All retries failed
        throw new IOException("Failed to fetch " + url + " after " + maxRetries + " attempts", lastException);
    }

    /**
     * Perform the actual HTTP request
     */
    private Document performRequest(String url) throws IOException {
        Connection connection = Jsoup.connect(url)
                .userAgent(getNextUserAgent())
                .timeout(20000) // 20 seconds timeout
                .followRedirects(true)
                .maxBodySize(10 * 1024 * 1024) // 10MB max
                .ignoreHttpErrors(false)
                .ignoreContentType(true);

        // Add realistic headers to avoid detection
        connection.header("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        connection.header("Accept-Language", "en-US,en;q=0.9");
        // Use identity to avoid compression issues (gzip/br) which can cause binary
        // garbage or empty parsing
        connection.header("Accept-Encoding", "identity");
        connection.header("Connection", "keep-alive");
        connection.header("Upgrade-Insecure-Requests", "1");
        connection.header("Sec-Fetch-Dest", "document");
        connection.header("Sec-Fetch-Mode", "navigate");
        connection.header("Sec-Fetch-Site", "none");
        connection.header("Sec-Fetch-User", "?1");
        connection.header("DNT", "1");
        connection.header("Cache-Control", "no-cache");
        connection.header("Pragma", "no-cache");
        connection.header("TE", "Trailers");

        return connection.get();
    }

    /**
     * Apply rate limiting per domain with random delay variation
     */
    private void applyRateLimit(String domain, long delayMs) {
        Long lastTime = lastRequestTime.get(domain);
        long currentTime = System.currentTimeMillis();

        if (lastTime != null) {
            long timeSinceLastRequest = currentTime - lastTime;

            // Add random variation to delay (±30%) to avoid detection patterns
            long randomVariation = (long) (delayMs * (Math.random() * 0.6 - 0.3)); // -30% to +30%
            long actualDelay = delayMs + randomVariation;
            actualDelay = Math.max(MIN_DELAY_MS, Math.min(MAX_DELAY_MS, actualDelay)); // Clamp to min/max

            if (timeSinceLastRequest < actualDelay) {
                long waitTime = actualDelay - timeSinceLastRequest;
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        lastRequestTime.put(domain, System.currentTimeMillis());

        // Track request count (reset after 1 minute)
        requestCounts.put(domain, requestCounts.getOrDefault(domain, 0) + 1);
    }

    /**
     * Check if error is a rate limit or anti-bot error
     */
    private boolean isRateLimitError(IOException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("429") ||
                lowerMessage.contains("rate limit") ||
                lowerMessage.contains("too many requests") ||
                lowerMessage.contains("quota exceeded") ||
                lowerMessage.contains("403") ||
                lowerMessage.contains("forbidden") ||
                lowerMessage.contains("cloudflare") ||
                lowerMessage.contains("access denied") ||
                lowerMessage.contains("blocked");
    }

    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Get next user agent (rotation)
     */
    private String getNextUserAgent() {
        String agent = USER_AGENTS[userAgentIndex];
        userAgentIndex = (userAgentIndex + 1) % USER_AGENTS.length;
        return agent;
    }

    /**
     * Clear cache for a specific URL
     */
    public void clearCache(String url) {
        documentCache.remove(url);
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        documentCache.clear();
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedDocuments", documentCache.size());
        stats.put("activeDomains", lastRequestTime.size());
        return stats;
    }

    /**
     * Cached document wrapper
     */
    private static class CachedDocument {
        final Document document;
        final long timestamp;

        CachedDocument(Document document, long timestamp) {
            this.document = document;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }
}
