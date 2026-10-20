package com.qa.proton.pages;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.qa.proton.config.Config;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Mailbox shell: folders, message list, message actions, message view. */
public class MailboxPage {
    private final Page page;

    public MailboxPage(Page page) {
        this.page = page;
    }

    // ---------- navigation ----------
    /**
     * Goes to the Inbox. When the tab is already inside the mailbox we use the in-app Inbox link (like a real user)
     * instead of a full page load; full reloads are only done by the tests that explicitly test refresh behaviour.
     */
    public MailboxPage open() {
        if (page.url().startsWith(Config.BASE_URL) && page.locator(Locators.folderLink(Locators.F_INBOX)).count() > 0) {
            page.locator(Locators.folderLink(Locators.F_INBOX)).first().click();
        } else {
            page.navigate(Config.BASE_URL + "/u/0/inbox");
        }
        assertThat(page.locator(Locators.COMPOSE_BUTTON).first()).isVisible();
        return dismissOverlays();
    }

    /**
     * New accounts show one-off welcome/onboarding dialogs that would block clicks. Closes any that are open.
     * Only touches dialogs with a clearly dismissive button, so it can never click Send/Delete.
     */
    public MailboxPage dismissOverlays() {
        Pattern dismiss = Pattern.compile("^(Got it|Skip|Maybe later|Not now|Close|Next|Continue|Start using Proton Mail|Let's get started)$",
                Pattern.CASE_INSENSITIVE);
        for (int i = 0; i < 4; i++) {
            Locator dialog = page.getByRole(AriaRole.DIALOG).first();
            if (dialog.count() == 0 || !dialog.isVisible()) break;
            Locator button = dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(dismiss));
            if (button.count() == 0) break;
            button.first().click();
            try {
                dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(3000));
            } catch (PlaywrightException ignored) {
                // another dialog may have replaced it; loop again
            }
        }
        return this;
    }

    public MailboxPage goTo(String folderId) {
        page.locator(Locators.folderLink(folderId)).click();
        assertThat(page).hasURL(Pattern.compile(".*/(" + urlSegment(folderId) + ").*"));
        return this;
    }

    private static String urlSegment(String folderId) {
        return switch (folderId) {
            case Locators.F_DRAFTS -> "drafts";
            case Locators.F_SENT -> "sent";
            default -> folderId;
        };
    }

    public ComposerPage compose() {
        return new ComposerPage(page).open();
    }

    // ---------- list ----------
    public Locator row(String subject) {
        return page.locator(Locators.LIST_ROW).filter(new Locator.FilterOptions().setHasText(subject));
    }

    /**
     * Mail delivery is asynchronous. Instead of a fixed sleep we wait for the row in short slices and
     * refresh the folder between slices until the delivery timeout expires.
     */
    public MailboxPage waitForMessage(String subject) {
        long deadline = System.currentTimeMillis() + Config.DELIVERY_TIMEOUT_MS;
        while (true) {
            try {
                row(subject).first().waitFor(new Locator.WaitForOptions().setTimeout(8000));
                return this;
            } catch (TimeoutError e) {
                if (System.currentTimeMillis() > deadline) {
                    throw new AssertionError("Message with subject '" + subject + "' did not arrive within "
                            + Config.DELIVERY_TIMEOUT_MS + " ms", e);
                }
                refresh();
            }
        }
    }

    public MailboxPage refresh() {
        Locator btn = page.locator(Locators.REFRESH_BUTTON).first();
        if (btn.count() > 0 && btn.isVisible()) btn.click();
        else page.reload();
        return this;
    }

    public MailboxPage select(String subject) {
        Locator r = row(subject).first();
        r.hover();
        r.locator(Locators.ROW_CHECKBOX).first().check();
        return this;
    }

    public MailboxPage open(String subject) {
        row(subject).first().click();
        return this;
    }

    // ---------- actions ----------
    public MailboxPage markRead() {
        page.locator(Locators.TOOLBAR_MARK_READ).click();
        return this;
    }

    public MailboxPage markUnread() {
        page.locator(Locators.TOOLBAR_MARK_UNREAD).click();
        return this;
    }

    public MailboxPage star(String subject) {
        Locator r = row(subject).first();
        r.hover();
        r.locator(Locators.ROW_STAR).first().click();
        return this;
    }

    public MailboxPage moveToTrash() {
        page.locator(Locators.TOOLBAR_TRASH).click();
        return this;
    }

    public MailboxPage moveToInbox() {
        page.locator(Locators.TOOLBAR_MOVE_TO_INBOX).click();
        return this;
    }

    // ---------- assertions (state, not just visibility) ----------
    public void assertPresent(String subject) {
        assertThat(row(subject).first()).isVisible();
    }

    public void assertAbsent(String subject) {
        assertThat(row(subject)).hasCount(0);
    }

    public void assertUnread(String subject, boolean unread) {
        Pattern p = Pattern.compile("(^|\\s)" + Locators.ROW_UNREAD_CLASS + "(\\s|$)");
        if (unread) assertThat(row(subject).first()).hasClass(p);
        else assertThat(row(subject).first()).not().hasClass(p);
    }

    // ---------- message view ----------
    public Locator openedBody() {
        return page.frameLocator(Locators.CONTENT_IFRAME).locator("body");
    }

    public Locator attachmentNamed(String fileName) {
        return page.locator(Locators.ATTACHMENT_ITEM).filter(new Locator.FilterOptions().setHasText(fileName));
    }

    public Download downloadAttachment(String fileName) {
        return page.waitForDownload(() -> attachmentNamed(fileName).first()
                .locator(Locators.ATTACHMENT_DOWNLOAD).first().click());
    }

    // ---------- session ----------
    public void logout() {
        page.locator(Locators.USER_DROPDOWN).click();
        page.locator(Locators.LOGOUT_BUTTON).click();
    }
}