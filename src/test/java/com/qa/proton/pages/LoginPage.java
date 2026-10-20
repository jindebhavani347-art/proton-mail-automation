package com.qa.proton.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.qa.proton.config.Config;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginPage {
    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public LoginPage open() {
        page.navigate(Config.LOGIN_URL);
        assertThat(page.locator(Locators.LOGIN_USERNAME)).isVisible();
        return this;
    }

    public LoginPage submitCredentials(String email, String password) {
        page.locator(Locators.LOGIN_USERNAME).fill(email);
        page.locator(Locators.LOGIN_PASSWORD).fill(password);
        keepMeSignedIn();
        page.locator(Locators.LOGIN_SUBMIT).click();
        return this;
    }

    /**
     * Proton keeps the session key only in the current tab unless "Keep me signed in" is ticked. Without it a saved
     * browser state is rejected with reason=session-expired, so cached sessions only work when this box is checked.
     */
    private void keepMeSignedIn() {
        Locator keep = page.getByLabel("Keep me signed in");
        if (keep.count() == 0) return;
        try {
            if (!keep.first().isChecked()) keep.first().check(new Locator.CheckOptions().setForce(true));
        } catch (PlaywrightException e) {
            page.getByText("Keep me signed in").first().click();     // custom toggle: click its label instead
        }
    }

    /** Waits for the mailbox shell. Long timeout so a human can solve a CAPTCHA when running headed. */
    public MailboxPage loginAs(Config.Account account) {
        open().submitCredentials(account.email(), account.password());
        Locator compose = page.locator(Locators.COMPOSE_BUTTON).first();
        compose.waitFor(new Locator.WaitForOptions().setTimeout(Config.LOGIN_TIMEOUT_MS));
        return new MailboxPage(page).dismissOverlays();
    }

    public Locator errorMessage() {
        return page.locator(Locators.LOGIN_ERROR).first();
    }

    public Locator usernameField() {
        return page.locator(Locators.LOGIN_USERNAME);
    }
}