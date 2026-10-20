package com.qa.proton.core;

import com.microsoft.playwright.*;
import com.qa.proton.config.Config;
import com.qa.proton.pages.ComposerPage;
import com.qa.proton.pages.MailboxPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Test base class.
 *  - ONE browser for the whole run; each account is logged in once and its tab is reused (see SessionManager)
 *  - tracing runs per test (chunk); the trace is KEPT only when the test fails (reports/traces/*.zip)
 *  - failure evidence: full-page screenshot + trace per failed test
 */
public abstract class BaseTest {

    private static Playwright playwright;
    private static Browser browser;

    private final List<BrowserContext> tracedContexts = new ArrayList<>();
    private final List<BrowserContext> ownedContexts = new ArrayList<>();   // closed after every test
    private String currentTest = "test";
    private boolean failed;

    @RegisterExtension
    final AfterTestExecutionCallback evidence = ctx -> {
        currentTest = ctx.getRequiredTestClass().getSimpleName() + "." + ctx.getRequiredTestMethod().getName();
        failed = ctx.getExecutionException().isPresent();
        if (failed) captureScreenshots();
    };

    @BeforeAll
    static synchronized void launchBrowser() {
        Config.requireAccounts();
        if (browser != null) return;
        playwright = Playwright.create();
        BrowserType type = switch (Config.BROWSER.toLowerCase()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
        browser = type.launch(new BrowserType.LaunchOptions().setHeadless(Config.HEADLESS));
        Browser b = browser;
        Playwright pw = playwright;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SessionManager.closeAll();
            try {
                b.close();
                pw.close();
            } catch (Exception ignored) {
                // JVM is exiting
            }
        }));
    }

    @AfterEach
    void stopTracingAndCloseOwned() {
        int i = 0;
        for (BrowserContext c : tracedContexts) {
            try {
                if (failed) {
                    Path dir = Path.of("reports", "traces");
                    Files.createDirectories(dir);
                    c.tracing().stopChunk(new Tracing.StopChunkOptions()
                            .setPath(dir.resolve(currentTest + "-" + (i++) + ".zip")));
                } else {
                    c.tracing().stopChunk();
                }
            } catch (Exception ignored) {
                // never let evidence collection hide the real test result
            }
        }
        tracedContexts.clear();
        for (BrowserContext c : ownedContexts) {
            try {
                c.close();
            } catch (Exception ignored) {
                // already closed
            }
        }
        ownedContexts.clear();
    }

    // ---------- page accessors ----------
    protected Page senderPage() {
        return track(SessionManager.sharedSession(browser, Config.SENDER));
    }

    protected Page receiverPage() {
        return track(SessionManager.sharedSession(browser, Config.RECEIVER));
    }

    /** A SECOND independent login of the same account (proves state is stored server-side). */
    protected Page secondSenderPage() {
        return ownedSession(Config.SENDER);
    }

    protected Page secondReceiverPage() {
        return ownedSession(Config.RECEIVER);
    }

    /** Un-authenticated page, used by the authentication tests. */
    protected Page anonymousPage() {
        BrowserContext ctx = SessionManager.newAnonymousContext(browser);
        ownedContexts.add(ctx);
        track(ctx);
        return ctx.newPage();
    }

    private Page ownedSession(Config.Account account) {
        Page page = SessionManager.newIndependentSession(browser, account);
        ownedContexts.add(page.context());
        return track(page);
    }

    private Page track(Page page) {
        track(page.context());
        return page;
    }

    private void track(BrowserContext ctx) {
        if (!tracedContexts.contains(ctx)) {
            tracedContexts.add(ctx);
            ctx.tracing().startChunk();
        }
    }

    private void captureScreenshots() {
        try {
            Path dir = Path.of("reports", "screenshots");
            Path diag = Path.of("reports", "diagnostics");
            Files.createDirectories(dir);
            Files.createDirectories(diag);
            int i = 0;
            for (BrowserContext c : tracedContexts) {
                for (Page p : c.pages()) {
                    String name = currentTest + "-" + (i++);
                    p.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(name + ".png")).setFullPage(true));
                    Files.writeString(diag.resolve(name + ".txt"), describePage(p));
                }
            }
        } catch (Exception ignored) {
            // best effort
        }
    }

    /** URL + every data-testid + every button label currently on the page: makes selector problems quick to fix. */
    private String describePage(Page p) {
        StringBuilder sb = new StringBuilder("URL: " + p.url() + "\nTITLE: " + p.title() + "\n\n== data-testid values ==\n");
        try {
            sb.append(p.evaluate("() => [...new Set([...document.querySelectorAll('[data-testid]')]"
                    + ".map(e => e.getAttribute('data-testid')))].sort().join('\\n')"));
            sb.append("\n\n== button labels ==\n");
            sb.append(p.evaluate("() => [...new Set([...document.querySelectorAll('button')]"
                    + ".map(b => (b.innerText || b.getAttribute('aria-label') || b.title || '').trim())"
                    + ".filter(Boolean))].sort().join('\\n')"));
        } catch (Exception e) {
            sb.append("could not read page: ").append(e.getMessage());
        }
        return sb.toString();
    }

    // ---------- shared workflow helpers ----------
    /** Sends a mail from the sender mailbox to the receiver and waits until it is in the receiver's inbox. */
    protected void seedReceiverInbox(String subject, String body) {
        new MailboxPage(senderPage()).open();
        ComposerPage composer = new MailboxPage(senderPage()).compose();
        composer.to(Config.RECEIVER.email()).subject(subject).body(body).send().assertSent();
        new MailboxPage(receiverPage()).open().waitForMessage(subject);
    }
}