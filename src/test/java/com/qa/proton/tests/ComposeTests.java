package com.qa.proton.tests;

import com.qa.proton.config.Config;
import com.qa.proton.core.BaseTest;
import com.qa.proton.pages.ComposerPage;
import com.qa.proton.pages.Locators;
import com.qa.proton.pages.MailboxPage;
import com.qa.proton.utils.Metrics;
import com.qa.proton.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class ComposeTests extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("COMP-01 two-account E2E: sender sends, receiver gets subject and body (delivery latency recorded)")
    void sendMailReachesReceiver() {
        String subject = TestData.subject("e2e");
        String body = TestData.body("e2e");

        MailboxPage senderBox = new MailboxPage(senderPage()).open();
        ComposerPage composer = senderBox.compose();
        composer.to(Config.RECEIVER.email()).subject(subject).body(body);

        long sentAt = System.nanoTime();
        composer.send().assertSent();

        MailboxPage receiverBox = new MailboxPage(receiverPage()).open().waitForMessage(subject);
        Metrics.record("delivery_latency_ms", (System.nanoTime() - sentAt) / 1_000_000);   // TIME-01 evidence

        receiverBox.open(subject);
        assertThat(receiverBox.openedBody()).containsText(body);
        senderBox.goTo(Locators.F_SENT).assertPresent(subject);     // post-send state on the sender side
    }

    @Test
    @Tag("smoke")
    @DisplayName("COMP-02 invalid recipient is rejected and nothing is sent")
    void invalidRecipientBlocksSend() {
        String subject = TestData.subject("invalid-rcpt");
        MailboxPage box = new MailboxPage(senderPage()).open();
        ComposerPage composer = box.compose();
        composer.to("not-an-email-address").subject(subject).body("x").send();

        assertThat(composer.recipientError()).isVisible();
        assertThat(composer.composer()).isVisible();                // composer stays open
        assertThat(composer.sentToast()).hasCount(0);

        composer.close();                                           // leave the mailbox clean
        box.goTo(Locators.F_SENT).assertAbsent(subject);
    }

    /**
     * Hypothesis list: each value is expected to be rejected. Run once, and if Proton legitimately accepts one,
     * remove it and record the behaviour as an observation (not a defect) in docs/DEFECT_REPORT.md.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" is rejected")
    @ValueSource(strings = {"plainaddress", "@no-local-part.com", "two@@signs.com", "missing-domain@", "dots@example..com"})
    @DisplayName("COMP-07 malformed recipient variants are rejected")
    void malformedRecipientsAreRejected(String badAddress) {
        String subject = TestData.subject("bad-rcpt");
        MailboxPage box = new MailboxPage(senderPage()).open();
        ComposerPage composer = box.compose();
        composer.to(badAddress).subject(subject).body("x").send();

        assertThat(composer.composer()).isVisible();                // not sent: composer still open
        assertThat(composer.sentToast()).hasCount(0);
        composer.close();
        box.goTo(Locators.F_SENT).assertAbsent(subject);            // and nothing reached Sent
    }

    @Test
    @DisplayName("COMP-11 BCC recipient receives the mail but is not exposed in the To recipient's copy")
    void bccRecipientIsHidden() {
        String subject = TestData.subject("bcc");
        String body = TestData.body("bcc");

        MailboxPage senderBox = new MailboxPage(senderPage()).open();
        senderBox.compose().to(Config.SENDER.email()).bcc(Config.RECEIVER.email())
                .subject(subject).body(body).send().assertSent();

        new MailboxPage(receiverPage()).open().waitForMessage(subject).assertPresent(subject);   // BCC delivered

        senderBox.goTo(Locators.F_INBOX).waitForMessage(subject).open(subject);                    // the "To" copy
        assertThat(senderBox.openedBody()).containsText(body);                                     // message fully loaded
        assertThat(senderPage().locator("body")).not().containsText(Config.RECEIVER.email());      // BCC not leaked
    }
}
