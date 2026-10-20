package com.qa.proton.tests;

import com.qa.proton.config.Config;
import com.qa.proton.core.BaseTest;
import com.qa.proton.pages.MailboxPage;
import com.qa.proton.pages.SearchPanel;
import com.qa.proton.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Search runs on the receiver mailbox. Proton indexes mail for search in the background
 * (client-side encrypted search), so results are asserted with auto-retrying checks, never instantly.
 */
class SearchTests extends BaseTest {

    @Test
    @DisplayName("SEARCH-01 keyword search finds the message and excludes others")
    void keywordSearch() {
        String match = TestData.subject("srch-match");
        String other = TestData.subject("srch-other");
        seedReceiverInbox(match, TestData.body("a"));
        seedReceiverInbox(other, TestData.body("b"));

        MailboxPage box = new MailboxPage(receiverPage());
        new SearchPanel(receiverPage()).open().keyword(match).submit();
        box.assertPresent(match);
        box.assertAbsent(other);
    }

    @Test
    @DisplayName("SEARCH-02 advanced multi-condition: keyword + sender + folder")
    void advancedMultiConditionSearch() {
        String subject = TestData.subject("srch-adv");
        seedReceiverInbox(subject, TestData.body("adv"));

        MailboxPage box = new MailboxPage(receiverPage());
        new SearchPanel(receiverPage()).open()
                .keyword(subject).from(Config.SENDER.email()).inFolder("Inbox").submit();
        box.assertPresent(subject);

        // same keyword but a sender that cannot match -> must return nothing (AND semantics)
        SearchPanel panel = new SearchPanel(receiverPage()).open();
        panel.keyword(subject).from("nobody-" + System.nanoTime() + "@example.com").submit();
        assertThat(panel.noResults()).isVisible();
    }
}
