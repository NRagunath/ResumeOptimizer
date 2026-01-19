package com.resumeopt;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.time.Duration;

public class SeleniumSetupTest {
    public static void main(String[] args) {
        System.out.println("Starting Selenium Setup Verification...");
        WebDriver driver = null;
        try {
            // 1. Setup WebDriverManager
            System.out.println("Setting up WebDriverManager...");
            WebDriverManager.chromedriver().setup();

            // 2. Configure Chrome Options
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            // 3. Initialize Driver
            System.out.println("Initializing ChromeDriver...");
            driver = new ChromeDriver(options);
            
            // 4. Configure Timeouts
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

            // Test 1: Google (Sanity Check)
            System.out.println("\n--- Test 1: Google Sanity Check ---");
            driver.get("https://www.google.com");
            System.out.println("Google Title: " + driver.getTitle());

            // Test 2: Target Portal (Naukri)
            System.out.println("\n--- Test 2: Naukri Portal Check ---");
            String url = "https://www.naukri.com/software-engineer-jobs-in-india";
            System.out.println("Navigating to: " + url);
            driver.get(url);
            
            // Wait a bit for JS to load
            Thread.sleep(3000);

            String title = driver.getTitle();
            String currentUrl = driver.getCurrentUrl();
            System.out.println("Page Title: " + title);
            System.out.println("Current URL: " + currentUrl);
            
            String pageSource = driver.getPageSource();
            if (pageSource != null && pageSource.length() > 500) {
                System.out.println("Page Source Snippet: " + pageSource.substring(0, 500).replace("\n", " "));
            } else {
                System.out.println("Page Source: " + pageSource);
            }

            if (title != null && !title.isEmpty()) {
                System.out.println("SUCCESS: Connected to portal.");
            } else {
                System.out.println("WARNING: Title is empty. Potential anti-bot blocking.");
            }

        } catch (Exception e) {
            System.err.println("ERROR: Selenium test failed.");
            e.printStackTrace();
        } finally {
            // 7. Resource Cleanup
            if (driver != null) {
                System.out.println("\nClosing browser session...");
                driver.quit();
                System.out.println("Browser closed.");
            }
        }
    }
}
