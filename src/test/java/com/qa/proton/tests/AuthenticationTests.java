package com.qa.proton.tests;

import com.microsoft.playwright.Page;
import com.qa.proton.config.Config;
import com.qa.proton.core.BaseTest;
import com.qa.proton.pages.LoginPage;
import com.qa.proton.pages.Locators;
import com.qa.proton.pages.MailboxPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Authentication & session. These tests use a FRESH (anonymous) browser context on purpose:
 * logging out of the cached session would invalidate it for every other test.
 */
class AuthenticationTests extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("AUTH-01 valid credentials open the mailbox")
    void validLoginOpensMailbox() {
        Page page = anonymousPage();
        MailboxPage mailbox = new LoginPage(page).loginAs(Config.SENDER);
        assertThat(page.locator(Locators.COMPOSE_BUTTON)).isVisible();
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*mail\\.proton\\.me.*"));
        mailbox.goTo(Locators.F_INBOX);
    }

    @Test
    @DisplayName("AUTH-02 wrong password shows an error and does not create a session")
    void wrongPasswordIsRejected() {
        Page page = anonymousPage();
        LoginPage login = new LoginPage(page).open();
        login.submitCredentials(Config.SENDER.email(), "definitely-wrong-" + System.nanoTime());
        assertThat(login.errorMessage()).isVisible();
        assertThat(page.locator(Locators.COMPOSE_BUTTON)).hasCount(0);   // never reached the mailbox
        assertThat(login.usernameField()).isVisible();                    // still on the login form
    }

    @Test
    @DisplayName("AUTH-03 logout ends the session and Back does not reopen the mailbox")
    void logoutThenBackDoesNotRestoreSession() {
        Page page = anonymousPage();
        MailboxPage mailbox = new LoginPage(page).loginAs(Config.SENDER);
        mailbox.logout();
        assertThat(page.locator(Locators.LOGIN_USERNAME)).isVisible();

        page.goBack();
        assertThat(page.locator(Locators.COMPOSE_BUTTON)).hasCount(0);
        assertThat(page.locator(Locators.LOGIN_USERNAME)).isVisible();
    }
}
