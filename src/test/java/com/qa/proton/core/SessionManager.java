package com.qa.proton.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.qa.proton.config.Config;
import com.qa.proton.pages.LoginPage;

import java.util.HashMap;
import java.util.Map;

/**
 * Proton rejects a saved/re-loaded browser state with reason=session-expired (part of the session key lives only in
 * the original tab). So instead of caching cookies we log in ONCE per account through the real UI and keep that same
 * logged-in tab open for the whole run. Tests navigate inside it, exactly like a real user who stays signed in.
 *
 *  - sharedSession(): one logged-in tab per account for the entire test run (2 logins in total)
 *  - newIndependentSession(): an extra, separate login of the same account (used to prove server-side state)
 *  - newAnonymousContext(): clean browser for the authentication tests
 */
public final class SessionManager {
    private static final Map<String, Page> SHARED_PAGES = new HashMap<>();

    private SessionManager() {}

    public static synchronized Page sharedSession(Browser browser, Config.Account account) {
        Page page = SHARED_PAGES.get(account.role());
        if (page == null || page.isClosed()) {
            page = newIndependentSession(browser, account);
            SHARED_PAGES.put(account.role(), page);
        }
        return page;
    }

    public static Page newIndependentSession(Browser browser, Config.Account account) {
        BrowserContext context = newAnonymousContext(browser);
        Page page = context.newPage();
        new LoginPage(page).loginAs(account);
        return page;
    }

    public static BrowserContext newAnonymousContext(Browser browser) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        context.setDefaultTimeout(Config.TIMEOUT_MS);
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        return context;
    }

    public static synchronized void closeAll() {
        for (Page page : SHARED_PAGES.values()) {
            try {
                page.context().close();
            } catch (Exception ignored) {
                // already closed
            }
        }
        SHARED_PAGES.clear();
    }
}