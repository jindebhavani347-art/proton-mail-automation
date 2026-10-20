package com.qa.proton.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Compose window component. */
public class ComposerPage {
    private final Page page;

    public ComposerPage(Page page) {
        this.page = page;
    }

    public ComposerPage open() {
        page.locator(Locators.COMPOSE_BUTTON).first().click();
        assertThat(page.locator(Locators.COMPOSER).first()).isVisible();
        return this;
    }

    public ComposerPage to(String email) {
        return addRecipient(Locators.COMPOSER_TO, email);
    }

    public ComposerPage cc(String email) {
        revealCcBcc();
        return addRecipient(Locators.COMPOSER_CC, email);
    }

    public ComposerPage bcc(String email) {
        revealCcBcc();
        return addRecipient(Locators.COMPOSER_BCC, email);
    }

    private void revealCcBcc() {
        Locator toggle = page.locator(Locators.COMPOSER_CC_BCC_TOGGLE).first();
        if (!page.locator(Locators.COMPOSER_CC).isVisible()) toggle.click();
    }

    private ComposerPage addRecipient(String selector, String email) {
        Locator input = page.locator(selector).first();
        input.fill(email);
        input.press("Enter");                       // commits the recipient as a chip
        return this;
    }

    public ComposerPage subject(String subject) {
        page.locator(Locators.COMPOSER_SUBJECT).fill(subject);
        return this;
    }

    public ComposerPage body(String text) {
        Locator editor = page.frameLocator(Locators.COMPOSER_BODY_IFRAME).locator("body");
        editor.click();
        editor.fill(text);
        return this;
    }

    public ComposerPage attach(Path file) {
        page.locator(Locators.COMPOSER_FILE_INPUT).first().setInputFiles(file);
        return this;
    }

    public Locator attachment(String fileName) {
        return page.locator(Locators.COMPOSER_ATTACHMENT_ITEM)
                .filter(new Locator.FilterOptions().setHasText(fileName));
    }

    public ComposerPage send() {
        page.locator(Locators.COMPOSER_SEND).click();
        return this;
    }

    /** Draft state is saved asynchronously: wait for Proton's own "saved" indicator, never a fixed sleep. */
    public ComposerPage waitForDraftSaved() {
        assertThat(page.locator(Locators.COMPOSER_SAVED_INDICATOR).first()).isVisible();
        return this;
    }

    public void close() {
        page.locator(Locators.COMPOSER_CLOSE).first().click();
    }

    public Locator subjectField() {
        return page.locator(Locators.COMPOSER_SUBJECT);
    }

    public Locator composer() {
        return page.locator(Locators.COMPOSER).first();
    }

    /** Text-based on purpose: the wording matters more to a user than the CSS class carrying it. */
    public Locator recipientError() {
        return page.getByText(java.util.regex.Pattern.compile("invalid|not valid", java.util.regex.Pattern.CASE_INSENSITIVE)).first();
    }

    // ---------- toasts ----------
    public Locator sentToast() {
        return page.locator(Locators.TOAST).filter(new Locator.FilterOptions().setHasText("Message sent"));
    }

    public Locator undoButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Undo"));
    }

    public void assertSent() {
        assertThat(sentToast().first()).isVisible();
        assertThat(composer()).isHidden();
    }
}
