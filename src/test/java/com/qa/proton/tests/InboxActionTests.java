package com.qa.proton.tests;

import com.qa.proton.core.BaseTest;
import com.qa.proton.pages.Locators;
import com.qa.proton.pages.MailboxPage;
import com.qa.proton.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Message actions run on the RECEIVER mailbox; each test seeds its own uniquely-named message.
 * A new account already holds Proton welcome mails, so tests never assume an empty inbox or an absolute unread count.
 */
class InboxActionTests extends BaseTest {

    @Test
    @DisplayName("INBOX-01 mark as read / unread toggles message state")
    void markReadAndUnread() {
        String subject = TestData.subject("readstate");
        seedReceiverInbox(subject, TestData.body("readstate"));

        MailboxPage box = new MailboxPage(receiverPage());
        box.assertUnread(subject, true);                           // new mail starts unread
        box.select(subject).markRead();
        box.assertUnread(subject, false);
        box.select(subject).markUnread();
        box.assertUnread(subject, true);
    }

    @Test
    @Tag("smoke")
    @DisplayName("INBOX-03 trash then restore returns the message to Inbox")
    void trashAndRestore() {
        String subject = TestData.subject("trash");
        seedReceiverInbox(subject, TestData.body("trash"));

        MailboxPage box = new MailboxPage(receiverPage());
        box.select(subject).moveToTrash();
        box.assertAbsent(subject);
        box.goTo(Locators.F_TRASH).assertPresent(subject);

        box.select(subject).moveToInbox();
        box.assertAbsent(subject);                                  // gone from Trash
        box.goTo(Locators.F_INBOX).assertPresent(subject);          // back in Inbox
    }

    @Test
    @DisplayName("INBOX-06 read/unread state persists after reload and in a brand-new session")
    void readStatePersistsServerSide() {
        String subject = TestData.subject("persist");
        seedReceiverInbox(subject, TestData.body("persist"));

        MailboxPage box = new MailboxPage(receiverPage());
        box.select(subject).markRead();
        box.assertUnread(subject, false);
        receiverPage().reload();
        box.assertUnread(subject, false);                           // read survives a refresh

        box.select(subject).markUnread();
        box.assertUnread(subject, true);
        receiverPage().reload();
        box.assertUnread(subject, true);                            // user-made "unread" survives a refresh

        MailboxPage other = new MailboxPage(secondReceiverPage()).open();   // independent session = server state
        other.waitForMessage(subject);
        other.assertUnread(subject, true);
    }
}
