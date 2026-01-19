package com.resumeopt.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Enhanced Selenium Service with robust error handling, driver recovery, and
 * anti-detection.
 * Uses ThreadLocal to ensure thread safety for parallel scraping.
 */
@Service
public class SeleniumService {

    // ThreadLocal to hold a separate WebDriver instance for each thread
    private final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    // Keep track of all drivers for cleanup
    private final List<WebDriver> allDrivers = Collections.synchronizedList(new ArrayList<>());

    private static final long PAGE_LOAD_TIMEOUT = 90;
    private static final long IMPLICIT_WAIT = 15;
    private static final long SCRIPT_TIMEOUT = 30;

    public SeleniumService() {
        // Lazy initialization
    }

    /**
     * Initialize WebDriver for the current thread
     */
    private void initDriver() {
        if (driverThreadLocal.get() == null) {
            try {
                System.out.println("[" + LocalDateTime.now() + "] Initializing Selenium WebDriver for thread: "
                        + Thread.currentThread().getName());

                // Setup WebDriverManager
                try {
                    WebDriverManager.chromedriver().setup();
                } catch (Exception e) {
                    System.err.println("[" + LocalDateTime.now() + "] ChromeDriver setup failed: " + e.getMessage());
                    throw new RuntimeException("Failed to setup ChromeDriver.", e);
                }

                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new");
                options.addArguments("--disable-gpu");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--remote-allow-origins=*");
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--start-maximized");
                options.addArguments("--disable-blink-features=AutomationControlled");
                options.addArguments("--disable-extensions");
                options.addArguments(
                        "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

                options.setExperimentalOption("excludeSwitches",
                        new String[] { "enable-automation", "enable-logging" });
                options.setExperimentalOption("useAutomationExtension", false);

                WebDriver driver = new ChromeDriver(options);

                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(IMPLICIT_WAIT));
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(SCRIPT_TIMEOUT));

                // Anti-detection script
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                } catch (Exception ignored) {
                }

                driverThreadLocal.set(driver);
                allDrivers.add(driver);

                System.out.println("[" + LocalDateTime.now() + "] Selenium WebDriver initialized for thread: "
                        + Thread.currentThread().getName());

            } catch (Exception e) {
                System.err.println(
                        "[" + LocalDateTime.now() + "] Failed to initialize Selenium WebDriver: " + e.getMessage());
                throw new RuntimeException("Selenium initialization failed.", e);
            }
        }
    }

    /**
     * Fetch document with enhanced error handling and retry logic
     */
    public Document fetchDocument(String url) {
        int retryCount = 0;
        int maxRetries = 2;

        while (retryCount <= maxRetries) {
            try {
                return fetchDocumentInternal(url);
            } catch (Exception e) {
                retryCount++;
                System.err.println("[" + LocalDateTime.now() + "] Selenium fetch failed (attempt " + retryCount + "/"
                        + (maxRetries + 1) + "): " + e.getMessage());

                if (retryCount <= maxRetries) {
                    quitDriver(); // Restart driver for this thread
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw new RuntimeException("Failed to fetch URL with Selenium after retries: " + url, e);
                }
            }
        }
        throw new RuntimeException("Failed to fetch URL with Selenium: " + url);
    }

    /**
     * Internal fetch implementation
     */
    private Document fetchDocumentInternal(String url) {
        initDriver();
        WebDriver driver = driverThreadLocal.get();

        try {
            System.out.println("[" + LocalDateTime.now() + "] Selenium fetching: " + url);

            try {
                driver.get(url);
            } catch (org.openqa.selenium.TimeoutException e) {
                System.out.println("Page load timeout - attempting to parse partially loaded page");
            }

            // Scroll
            try {
                for (int i = 0; i < 3; i++) {
                    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, document.body.scrollHeight/3);");
                    Thread.sleep(1000);
                }
            } catch (Exception ignored) {
            }

            // Wait for render
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }

            // Try to extract content via JS first (often more reliable)
            String pageSource = driver.getPageSource();
            return Jsoup.parse(pageSource, url);

        } catch (Exception e) {
            throw new RuntimeException("Error fetching URL: " + e.getMessage(), e);
        }
    }

    /**
     * Get WebDriver instance for current thread
     */
    public WebDriver getDriver() {
        initDriver();
        return driverThreadLocal.get();
    }

    /**
     * Quit driver for current thread
     */
    public void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                allDrivers.remove(driver);
            } catch (Exception e) {
                System.err.println("Error closing WebDriver: " + e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }

    /**
     * Cleanup all drivers on shutdown
     */
    @PreDestroy
    public void cleanupAllDrivers() {
        System.out.println("Closing all Selenium drivers...");
        synchronized (allDrivers) {
            for (WebDriver driver : allDrivers) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    // Ignore
                }
            }
            allDrivers.clear();
        }
    }
}
