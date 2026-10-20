package com.qa.proton.tests;

import com.qa.proton.config.Config;
import com.qa.proton.core.BaseTest;
import com.qa.proton.pages.ComposerPage;
import com.qa.proton.pages.Locators;
import com.qa.proton.pages.MailboxPage;
import com.qa.proton.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class DraftTests extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("DRAFT-01 draft is auto-saved and survives a page reload")
    void draftPersistsAfterReload() {
        String subject = TestData.subject("draft");
        MailboxPage box = new MailboxPage(senderPage()).open();
        ComposerPage composer = box.compose();
        composer.to(Config.RECEIVER.email()).subject(subject).body(TestData.body("draft")).waitForDraftSaved();
        composer.close();

        box.goTo(Locators.F_DRAFTS).assertPresent(subject);
        senderPage().reload();                                     // refresh behaviour
        box.assertPresent(subject);

        box.open(subject);                                         // reopen and verify the saved content
        assertThat(composer.subjectField()).hasValue(subject);
    }

    @Test
    @DisplayName("DRAFT-02 sending a draft moves it from Drafts to Sent")
    void draftTransitionsToSent() {
        String subject = TestData.subject("draft-to-sent");
        MailboxPage box = new MailboxPage(senderPage()).open();
        ComposerPage composer = box.compose();
        composer.to(Config.RECEIVER.email()).subject(subject).body(TestData.body("d2s")).waitForDraftSaved();
        composer.close();

        box.goTo(Locators.F_DRAFTS).open(subject);
        composer.send().assertSent();

        box.goTo(Locators.F_DRAFTS).assertAbsent(subject);          // state transition: draft is gone
        box.goTo(Locators.F_SENT).assertPresent(subject);           // ...and now exists as sent
    }

    @Test
    @DisplayName("DRAFT-05 editing a draft updates it in place (no duplicate draft)")
    void editedDraftReplacesOldVersion() {
        String original = TestData.subject("draft-orig");
        String edited = TestData.subject("draft-edit");
        MailboxPage box = new MailboxPage(senderPage()).open();
        ComposerPage composer = box.compose();
        composer.to(Config.RECEIVER.email()).subject(original).body(TestData.body("edit")).waitForDraftSaved();
        composer.close();

        box.goTo(Locators.F_DRAFTS).open(original);
        composer.subjectField().fill(edited);
        composer.close();        // closing saves; we then verify the saved STATE (a "Saved" label could be stale here)

        box.goTo(Locators.F_INBOX).goTo(Locators.F_DRAFTS);
        box.assertPresent(edited);
        box.assertAbsent(original);                                 // old version must not linger as a duplicate
    }

    @Test
    @DisplayName("DRAFT-09 a draft is stored on the server: visible from a second browser session")
    void draftVisibleFromSecondSession() {
        String subject = TestData.subject("draft-sync");
        MailboxPage box = new MailboxPage(senderPage()).open();
        ComposerPage composer = box.compose();
        composer.to(Config.RECEIVER.email()).subject(subject).body(TestData.body("sync")).waitForDraftSaved();
        composer.close();

        new MailboxPage(secondSenderPage()).open().goTo(Locators.F_DRAFTS).assertPresent(subject);
    }

    @Test
    @DisplayName("ATT-08 a draft keeps its attachment after reload")
    void draftKeepsAttachment() {
        String subject = TestData.subject("draft-attach");
        Path file = TestData.attachmentFile("draft-attachment-" + System.nanoTime());
        String fileName = file.getFileName().toString();

        MailboxPage box = new MailboxPage(senderPage()).open();
        ComposerPage composer = box.compose();
        composer.to(Config.RECEIVER.email()).subject(subject).body(TestData.body("da")).attach(file);
        assertThat(composer.attachment(fileName).first()).isVisible();   // upload finished
        composer.close();

        box.goTo(Locators.F_DRAFTS).assertPresent(subject);
        senderPage().reload();
        box.open(subject);
        assertThat(composer.attachment(fileName).first()).isVisible();   // attachment persisted with the draft
    }
}
