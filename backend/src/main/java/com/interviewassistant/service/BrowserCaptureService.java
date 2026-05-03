package com.interviewassistant.service;

import com.interviewassistant.dto.import_.CaptureResponse;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class BrowserCaptureService {

    private static final int MAX_CONTENT_LENGTH = Integer.MAX_VALUE;
    private static final String PAGE_TIMEOUT_MS = "30000";
    private static final List<String> CONTENT_SELECTORS = List.of(
            "article", "main", ".post-body", ".article-content",
            ".content", ".post-content", "#content", "body"
    );

    private volatile com.microsoft.playwright.Playwright playwright;
    private volatile com.microsoft.playwright.Browser browser;

    public CaptureResponse capture(String url) {
        String rawUrl = toGitHubRawUrl(url);
        if (rawUrl != null) {
            return fetchRawMarkdown(rawUrl, url);
        }

        ensureBrowser();
        com.microsoft.playwright.BrowserContext context = browser.newContext(
                new com.microsoft.playwright.Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        );
        try {
            com.microsoft.playwright.Page page = context.newPage();

            page.navigate(url, new com.microsoft.playwright.Page.NavigateOptions()
                    .setTimeout(Double.parseDouble(PAGE_TIMEOUT_MS)));
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.LOAD,
                    new com.microsoft.playwright.Page.WaitForLoadStateOptions()
                            .setTimeout(Double.parseDouble(PAGE_TIMEOUT_MS)));

            String title = page.title();
            String content = extractContent(page);

            return new CaptureResponse(title, content, url, Instant.now().toString());
        } catch (Exception e) {
            throw new RuntimeException("网页抓取失败: " + e.getMessage(), e);
        } finally {
            context.close();
        }
    }

    String toGitHubRawUrl(String url) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "^https://github\\.com/([^/]+/[^/]+)/blob/(.+)$").matcher(url);
        if (!m.matches()) return null;
        return "https://raw.githubusercontent.com/" + m.group(1) + "/" + m.group(2);
    }

    private CaptureResponse fetchRawMarkdown(String rawUrl, String originalUrl) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(rawUrl))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .GET().build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("GitHub raw fetch failed: HTTP " + response.statusCode());
            }
            String content = truncate(response.body().trim());
            String title = originalUrl.substring(originalUrl.lastIndexOf('/') + 1);
            return new CaptureResponse(title, content, originalUrl, Instant.now().toString());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GitHub raw fetch failed: " + e.getMessage(), e);
        }
    }

    private String extractContent(com.microsoft.playwright.Page page) {
        String best = "";
        for (String selector : CONTENT_SELECTORS) {
            try {
                String text = page.locator(selector).first().innerText();
                if (text != null && text.trim().length() > best.length()) {
                    best = text.trim();
                }
            } catch (Exception ignored) {
            }
        }
        if (best.isEmpty()) {
            String bodyText = page.innerText("body");
            best = bodyText != null ? bodyText.trim() : "";
        }
        return truncate(best);
    }

    private String truncate(String text) {
        if (text.length() <= MAX_CONTENT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_CONTENT_LENGTH) + "\n...（内容过长，已截断）";
    }

    private synchronized void ensureBrowser() {
        if (browser == null) {
            log.info("Initializing Playwright browser...");
            playwright = com.microsoft.playwright.Playwright.create();
            browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true)
            );
            log.info("Playwright browser initialized");
        }
    }

    @PreDestroy
    public synchronized void destroy() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
