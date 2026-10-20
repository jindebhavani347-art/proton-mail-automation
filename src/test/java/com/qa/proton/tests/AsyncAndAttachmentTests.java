package com.qa.proton.tests;

import com.microsoft.playwright.Download;
import com.qa.proton.config.Config;
import com.qa.proton.core.BaseTest;
import com.qa.proton.pages.ComposerPage;
import com.qa.proton.pages.Locators;
import com.qa.proton.pages.MailboxPage;
import com.qa.proton.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AsyncAndAttachmentTests extends BaseTest {

    @Test
    @DisplayName("ASYNC-01 Undo Send cancels the send; re-sending then delivers exactly once")
    void undoSendThenResend() {
        String subject = TestData.subject("undo");
        MailboxPage senderBox = new MailboxPage(senderPage()).open();
        ComposerPage composer = senderBox.compose();
        composer.to(Config.RECEIVER.email()).subject(subject).body(TestData.body("undo")).send();

        // transient state: the Undo action is only available for a few seconds -> wait for it, no sleep
        assertThat(composer.undoButton().first()).isVisible();
        composer.undoButton().first().click();

        assertThat(composer.composer()).isVisible();                 // composer re-opens with the content
        assertThat(composer.subjectField()).hasValue(subject);
        senderBox.goTo(Locators.F_SENT).assertAbsent(subject);       // nothing was sent

        // final state: send again and let the undo window expire
        senderBox.goTo(Locators.F_DRAFTS).open(subject);
        composer.send();
        assertThat(composer.sentToast().first()).isVisible();
        assertThat(composer.sentToast()).hasCount(0);                // toast disappears = undo window closed
        senderBox.goTo(Locators.F_SENT).assertPresent(subject);
        new MailboxPage(receiverPage()).open().waitForMessage(subject).assertPresent(subject);
    }

    @Test
    @DisplayName("ATTACH-01 attachment is sent, received, and downloads with identical content")
    void attachmentRoundTrip() throws IOException {
        String subject = TestData.subject("attach");
        String content = "attachment-content-" + System.nanoTime();
        Path file = TestData.attachmentFile(content);
        String fileName = file.getFileName().toString();

        MailboxPage senderBox = new MailboxPage(senderPage()).open();
        ComposerPage composer = senderBox.compose();
        composer.to(Config.RECEIVER.email()).subject(subject).body(TestData.body("attach")).attach(file);
        assertThat(composer.attachment(fileName).first()).isVisible();   // upload completed
        composer.send().assertSent();

        MailboxPage receiverBox = new MailboxPage(receiverPage()).open().waitForMessage(subject);
        receiverBox.open(subject);
        assertThat(receiverBox.attachmentNamed(fileName).first()).isVisible();   // filename preserved

        Download download = receiverBox.downloadAttachment(fileName);
        org.assertj.core.api.Assertions.assertThat(Files.readString(download.path()))
                .isEqualTo(content);                                                 // content integrity
    }
}
