package com.resumeopt;

import com.resumeopt.service.SeleniumService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PortalVerificationTest {

    // Shared Selenium Service
    private static SeleniumService seleniumService;

    @BeforeAll
    public static void setup() {
        seleniumService = new SeleniumService();
    }

    @AfterAll
    public static void tearDown() {
        if (seleniumService != null) {
            seleniumService.quitDriver();
        }
    }

    @Test
    public void verifyAllPortals() {
        System.out.println("Starting Comprehensive Portal Verification...");
        System.out.println("--------------------------------------------------");

        Map<String, PortalTester> portals = new HashMap<>();

        // 1. Naukri (Selenium)
            portals.put("Naukri", new PortalTester() {
                @Override
                public String buildUrl() {
                     try {
                         // Standard URL
                         return "https://www.naukri.com/software-engineer-jobs-in-india";
                    } catch (Exception e) { return ""; }
                }
                @Override
                public String[] getSelectors() {
                    return new String[]{".jobTuple", ".srp-jobtuple-wrapper", "article.jobTuple", ".job-tuple", "[class*='jobTuple']", ".job-card", "[data-job-id]", ".job-listing", ".srp-jobtuple-wrapper"};
                }
                @Override
                public boolean useSelenium() { return true; }
            });

        // 2. Shine
        portals.put("Shine", new PortalTester() {
            @Override
            public String buildUrl() {
                // Default search URL pattern for Shine
                return "https://www.shine.com/job-search/software-engineer-jobs-in-india?q=software-engineer&loc=India&exp=0";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{".jobCard", ".job_listing", ".job-card", "[class*='job']", ".search-result", ".listing"};
            }
        });

        // 3. LinkedIn
        portals.put("LinkedIn", new PortalTester() {
            @Override
            public String buildUrl() {
                return "https://www.linkedin.com/jobs/search?keywords=software%20engineer&location=India&f_TPR=r86400&position=1&pageNum=0";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{".jobs-search__results-list li", ".job-search-card", ".base-card", ".jobs-search-results__list-item", "li[class*='job']", "div[class*='job-card']"};
            }
        });

        // 4. Jobsora
        portals.put("Jobsora", new PortalTester() {
            @Override
            public String buildUrl() {
                return "https://in.jobsora.com/jobs?q=software+engineer+entry+level&l=India";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{".vacancy", ".job-item", ".job-card", "[class*='job']", "[class*='vacancy']", ".search-result"};
            }
        });

        // 5. Internshala
        portals.put("Internshala", new PortalTester() {
            @Override
            public String buildUrl() {
                return "https://internshala.com/internships/software-development-internship/part-time-false/work-from-home-false/is_internship_challenge-false/";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{".individual_internship", ".internship_meta"};
            }
        });

        // 6. Wellfound
        portals.put("Wellfound", new PortalTester() {
            @Override
            public String buildUrl() {
                return "https://wellfound.com/jobs?q=software+engineer+fresher&location=India";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{"[data-test='JobListItem']", "[data-test='JobSearchResult']", ".job-listing", "div[class*='JobCard']"};
            }
            @Override
            public boolean useSelenium() { return true; }
        });

        // 7. Cutshort
        portals.put("Cutshort", new PortalTester() {
            @Override
            public String buildUrl() {
                return "https://cutshort.io/search-jobs?experience=0-1";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{".job-card", "div[class*='JobCard']", "div[class*='jobCard']", "a[href*='/job/']"};
            }
            @Override
            public boolean useSelenium() { return true; }
        });
        
        // 8. Hirist
        portals.put("Hirist", new PortalTester() {
            @Override
            public String buildUrl() {
                return "https://www.hirist.com/search/software-engineer-jobs";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{".job-title", ".job-row", ".job-card", "div[class*='job']"};
            }
            @Override
            public boolean useSelenium() { return true; }
        });

        // 9. Indeed
        portals.put("Indeed", new PortalTester() {
            @Override
            public String buildUrl() {
                return "https://www.indeed.co.in/jobs?q=software+engineer+entry+level&l=India";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{".job_seen_beacon", ".jobTitle", ".resultContent", "[class*='job']"};
            }
            @Override
            public boolean useSelenium() { return true; }
        });

        // 10. Glassdoor
        portals.put("Glassdoor", new PortalTester() {
            @Override
            public String buildUrl() {
                return "https://www.glassdoor.co.in/Job/india-software-engineer-jobs-SRCH_IL.0,5_IN115_KO6,23.htm";
            }
            @Override
            public String[] getSelectors() {
                return new String[]{"li[data-test='jobListing']", "[data-test='job-card-wrapper']", "a[data-test='job-title']", "li.JobsList_jobListItem__wjTHv", ".JobCard_jobCardWrapper__vX29z"};
            }
            @Override
            public boolean useSelenium() { return true; }
        });

        // 11. Freshersworld (Simplified URL)
            portals.put("Freshersworld", new PortalTester() {
                @Override
                public String buildUrl() {
                    return "https://www.freshersworld.com/jobs/jobsearch/software-engineer-jobs-in-india";
                }
                @Override
                public String[] getSelectors() {
                    return new String[]{".job-container", ".job-tittle", "article.job", ".list-container", ".job-list", ".job-posting", ".job-card"};
                }
            });

        for (Map.Entry<String, PortalTester> entry : portals.entrySet()) {
            System.out.println("\nTesting " + entry.getKey() + "...");
            verifyPortal(entry.getKey(), entry.getValue());
        }
    }

    private static void verifyPortal(String name, PortalTester tester) {
        String url = tester.buildUrl();
        if (url == null || url.isEmpty()) {
            System.out.println("STATUS: SKIPPED (URL construction failed)");
            return;
        }
        
        System.out.println("URL: " + url);
        
        try {
            Document doc;
            if (tester.useSelenium()) {
                // Use shared Selenium service
                doc = seleniumService.fetchDocument(url);
            } else {
                // Use Standard Jsoup
                Connection connection = Jsoup.connect(url)
                       .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                       .timeout(20000)
                       .followRedirects(true)
                       .ignoreHttpErrors(true)
                       .ignoreContentType(true);
                
                connection.header("Accept-Encoding", "identity"); // Fix compression issues
                
                if (tester.getHeaders() != null) {
                    connection.headers(tester.getHeaders());
                }
                
                doc = connection.get();
            }

            String title = doc.title();
            System.out.println("Page Title: " + title);

            // Anti-bot check
            String html = doc.html();
            if (title.contains("Cloudflare") || title.contains("Just a moment") || title.contains("Security Challenge")
                    || html.contains("challenges.cloudflare.com/turnstile") || html.contains("turnstile.render")) {
                System.out.println("STATUS: BLOCKED (Anti-bot detected)");
                return;
            }

            boolean found = false;
            for (String selector : tester.getSelectors()) {
                Elements elements = doc.select(selector);
                if (!elements.isEmpty()) {
                    System.out.println("STATUS: WORKING (Found " + elements.size() + " items with selector '" + selector + "')");
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("STATUS: FAILED (No items found with any selector)");
                // Print a small snippet for debugging
                String text = doc.body().text();
                System.out.println("Snippet: " + (text.length() > 200 ? text.substring(0, 200) : text));
                
                // Save HTML to file for inspection
                try {
                    java.nio.file.Files.write(java.nio.file.Paths.get("failed_" + name + ".html"), doc.html().getBytes());
                    System.out.println("Saved HTML to failed_" + name + ".html");
                } catch (IOException e) {
                    System.out.println("Failed to save HTML: " + e.getMessage());
                }
                
                // Debug: Check if "job" exists anywhere
                if (doc.html().toLowerCase().contains("job")) {
                     System.out.println("DEBUG: Page contains 'job' keyword, but selectors failed.");
                }
            }

        } catch (IOException e) {
            System.out.println("STATUS: ERROR (" + e.getMessage() + ")");
        }
        
        // Brief pause to be nice
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }

    interface PortalTester {
        String buildUrl();
        String[] getSelectors();
        default Map<String, String> getHeaders() {
            return new HashMap<>();
        }
        default boolean useSelenium() {
            return false;
        }
    }
}
