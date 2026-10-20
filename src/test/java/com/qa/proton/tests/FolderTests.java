package com.qa.proton.tests;

import com.qa.proton.core.BaseTest;
import com.qa.proton.pages.FoldersPage;
import com.qa.proton.pages.MailboxPage;
import com.qa.proton.utils.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FolderTests extends BaseTest {

    /** One reusable fixture folder: free plans cap the number of folders, so it is created once and kept. */
    private static final String FOLDER = "QA-Folder";

    @Test
    @DisplayName("FOLDER-01 message moved to a custom folder leaves Inbox and appears in that folder")
    void moveMessageToCustomFolder() {
        String subject = TestData.subject("folder");
        seedReceiverInbox(subject, TestData.body("folder"));

        FoldersPage folders = new FoldersPage(receiverPage()).ensureFolder(FOLDER);
        MailboxPage box = new MailboxPage(receiverPage());
        box.select(subject);
        folders.moveSelectedTo(FOLDER);

        box.assertAbsent(subject);                                  // no longer in Inbox
        folders.open(FOLDER);
        box.assertPresent(subject);                                 // now organised under the custom folder
    }
}
