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

    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final String PAGE_TIMEOUT_MS = "30000";
    private static final List<String> CONTENT_SELECTORS = List.of(
            "article", "main", ".post-body", ".article-content",
            ".content", ".post-content", "#content", "body"
    );

    private volatile com.microsoft.playwright.Playwright playwright;
    private volatile com.microsoft.playwright.Browser browser;

    public CaptureResponse capture(String url) {
        try {
            ensureBrowser();
            com.microsoft.playwright.BrowserContext context = browser.newContext(
                    new com.microsoft.playwright.Browser.NewContextOptions()
                            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            );
            com.microsoft.playwright.Page page = context.newPage();

            page.navigate(url, new com.microsoft.playwright.Page.NavigateOptions()
                    .setTimeout(Double.parseDouble(PAGE_TIMEOUT_MS)));
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                    new com.microsoft.playwright.Page.WaitForLoadStateOptions()
                            .setTimeout(Double.parseDouble(PAGE_TIMEOUT_MS)));

            String title = page.title();
            String content = extractContent(page);

            context.close();

            return new CaptureResponse(title, content, url, Instant.now().toString());
        } catch (Exception e) {
            throw new RuntimeException("网页抓取失败: " + e.getMessage(), e);
        }
    }

    private String extractContent(com.microsoft.playwright.Page page) {
        for (String selector : CONTENT_SELECTORS) {
            try {
                String text = page.locator(selector).first().innerText();
                if (text != null && text.length() > 100) {
                    return truncate(text.trim());
                }
            } catch (Exception ignored) {
            }
        }
        String bodyText = page.innerText("body");
        return truncate(bodyText != null ? bodyText.trim() : "");
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
