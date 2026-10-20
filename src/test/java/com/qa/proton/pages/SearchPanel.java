package com.qa.proton.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Header search bar + advanced-search panel. */
public class SearchPanel {
    private final Page page;

    public SearchPanel(Page page) {
        this.page = page;
    }

    private Locator keywordInput() {
        return page.locator(Locators.SEARCH_KEYWORD).first();
    }

    public SearchPanel open() {
        keywordInput().click();
        assertThat(keywordInput()).isVisible();
        return this;
    }

    public SearchPanel keyword(String text) {
        keywordInput().fill(text);
        return this;
    }

    /** Advanced fields live in a panel that may need to be expanded first. */
    private void ensureAdvancedVisible() {
        Locator from = page.locator(Locators.SEARCH_FROM).first();
        if (!from.isVisible()) {
            page.locator(Locators.SEARCH_ADVANCED_TOGGLE).first().click();
        }
        assertThat(from).isVisible();
    }

    public SearchPanel from(String sender) {
        ensureAdvancedVisible();
        page.locator(Locators.SEARCH_FROM).first().fill(sender);
        return this;
    }

    public SearchPanel inFolder(String label) {
        ensureAdvancedVisible();
        page.locator(Locators.SEARCH_FOLDER).first().selectOption(label);
        return this;
    }

    /** Uses the panel's Search button when shown, otherwise submits the keyword box with Enter. */
    public SearchPanel submit() {
        Locator button = page.locator(Locators.SEARCH_SUBMIT).first();
        if (button.isVisible()) {
            button.click();
        } else {
            keywordInput().press("Enter");
        }
        return this;
    }

    public SearchPanel reset() {
        page.locator(Locators.SEARCH_RESET).first().click();
        return this;
    }

    public Locator noResults() {
        return page.getByText("No results found");
    }
}
