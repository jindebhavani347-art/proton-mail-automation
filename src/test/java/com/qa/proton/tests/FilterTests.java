package com.qa.proton.tests;

import com.qa.proton.core.BaseTest;
import com.qa.proton.pages.FiltersPage;
import com.qa.proton.pages.Locators;
import com.qa.proton.pages.MailboxPage;
import com.qa.proton.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class FilterTests extends BaseTest {

    @Test
    @DisplayName("FILTER-01 subject filter archives matching mail only, then is deleted")
    void subjectFilterArchivesMatchingMailOnly() {
        String keyword = "autofilter" + UUID.randomUUID().toString().substring(0, 6);
        String filterName = "QA-filter-" + keyword;
        String matching = TestData.subject(keyword);
        String nonMatching = TestData.subject("plain");

        FiltersPage filters = new FiltersPage(receiverPage()).open();
        try {
            filters.createSubjectFilterArchive(filterName, keyword);

            seedReceiverInboxAllowArchive(matching);
            seedReceiverInbox(nonMatching, TestData.body("plain"));

            MailboxPage box = new MailboxPage(receiverPage()).open();
            box.assertPresent(nonMatching);                          // non-matching stays in Inbox
            box.assertAbsent(matching);                              // matching skipped Inbox...
            box.goTo(Locators.F_ARCHIVE).waitForMessage(matching);   // ...and was archived by the rule
        } finally {
            new FiltersPage(receiverPage()).open().deleteFilter(filterName);   // always clean up (free-plan limit)
        }
        assertThat(receiverPage().getByText(filterName)).hasCount(0);
    }

    /** A filtered mail never shows in Inbox, so we cannot reuse seedReceiverInbox (it waits for Inbox). */
    private void seedReceiverInboxAllowArchive(String subject) {
        new MailboxPage(senderPage()).open();
        com.qa.proton.pages.ComposerPage c = new MailboxPage(senderPage()).compose();
        c.to(com.qa.proton.config.Config.RECEIVER.email()).subject(subject).body(TestData.body("filtered")).send().assertSent();
    }
}
