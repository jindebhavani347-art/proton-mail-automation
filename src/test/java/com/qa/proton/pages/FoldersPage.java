package com.qa.proton.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Custom folders in the sidebar. Role/text based because custom folders have no stable test ids.
 * Free plans allow only a few folders, so a single fixture folder is created once and reused (never deleted).
 */
public class FoldersPage {
    private final Page page;

    public FoldersPage(Page page) {
        this.page = page;
    }

    private Locator sidebarFolder(String name) {
        return page.getByRole(AriaRole.NAVIGATION).getByText(name, new Locator.GetByTextOptions().setExact(true)).first();
    }

    /** Creates the folder only if it does not exist yet (idempotent fixture). */
    public FoldersPage ensureFolder(String name) {
        expandFolders();
        if (sidebarFolder(name).count() > 0) return this;
        page.locator(Locators.ADD_FOLDER).first().click();
        Locator dialog = page.getByRole(AriaRole.DIALOG).first();
        dialog.getByRole(AriaRole.TEXTBOX).first().fill(name);
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
        assertThat(sidebarFolder(name)).isVisible();
        return this;
    }

    private void expandFolders() {
        Locator toggle = page.getByRole(AriaRole.NAVIGATION)
                .getByText("Folders", new Locator.GetByTextOptions().setExact(true)).first();
        if (toggle.count() > 0 && page.locator(Locators.ADD_FOLDER).first().isVisible()) {
            // the + button is part of the Folders header; list may still be collapsed
            toggle.click();
        }
    }

    /** Moves the currently selected message(s) with the toolbar "Move to" action. */
    public FoldersPage moveSelectedTo(String folderName) {
        page.locator(Locators.TOOLBAR_MOVE_TO).click();
        page.getByText(folderName, new Page.GetByTextOptions().setExact(true)).last().click();
        return this;
    }

    public FoldersPage open(String folderName) {
        expandFolders();
        sidebarFolder(folderName).click();
        return this;
    }
}
