package com.qa.proton.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * ALL selectors live here. When Proton changes its UI, this is the only file to touch.
 *
 * Strategy: prefer data-testid / role / visible text over CSS classes or XPath.
 * NOTE: values below are best-known Proton Mail test ids. Verify them once against the live app
 * (see README -> "Verifying selectors with codegen") and adjust here if any differ.
 */
public final class Locators {
    private Locators() {}

    // ---------- Login ----------
    public static final String LOGIN_USERNAME = "#username";
    //public static  final String login_username_lable = page.getByText("Email or username");
    public static final String LOGIN_PASSWORD = "#password";
    public static  final String logoutpup = "button:has-text('Sign out')";
    public static final String LOGIN_SUBMIT = "button[type='submit']";
    public static final String LOGIN_ERROR = "[role='alert'], .notification--error, .input-element-container .color-danger";

    // ---------- Shell / navigation ----------
    public static final String COMPOSE_BUTTON = "[data-testid='sidebar:compose'], button:has-text('New message')";
    public static final String USER_DROPDOWN = "[data-testid='heading:userdropdown']";
    public static final String LOGOUT_BUTTON = "[data-testid='userdropdown:button:logout']";
    public static final String REFRESH_BUTTON = "[data-testid='navigation-link:refresh-folder'], [data-testid='toolbar:refresh']";

    public static String folderLink(String folderId) {
        return "[data-testid='navigation-link:" + folderId + "']";
    }

    // folder ids
    public static final String F_INBOX = "inbox";
    public static final String F_DRAFTS = "all-drafts";
    public static final String F_SENT = "all-sent";
    public static final String F_STARRED = "starred";
    public static final String F_ARCHIVE = "archive";
    public static final String F_TRASH = "trash";

    // ---------- Message list ----------
    public static final String LIST_ROW = "[data-shortcut-target='item-container']";
    public static final String ROW_CHECKBOX = "[data-testid='item-checkbox']";
    public static final String ROW_STAR = "[data-testid='icon-star'], [data-testid='item-star']";
    public static final String ROW_UNREAD_CLASS = "item-unread";
    public static final String TOOLBAR_TRASH = "[data-testid='toolbar:movetotrash']";
    public static final String TOOLBAR_ARCHIVE = "[data-testid='toolbar:movetoarchive']";
    public static final String TOOLBAR_MARK_READ = "[data-testid='toolbar:readbutton']";
    public static final String TOOLBAR_MARK_UNREAD = "[data-testid='toolbar:unreadbutton']";
    public static final String TOOLBAR_MOVE_TO_INBOX = "[data-testid='toolbar:movetoinbox']";
    public static final String TOOLBAR_MOVE_TO = "[data-testid='toolbar:moveto']";
    public static final String ADD_FOLDER = "[data-testid='navigation-link:add-folder'], button[title='Create a new folder']";
    public static final String EMPTY_STATE = "[data-testid='empty-view-placeholder']";

    // ---------- Message view ----------
    public static final String CONTENT_IFRAME = "[data-testid='content-iframe']";
    public static final String ATTACHMENT_LIST = "[data-testid='attachment-list']";
    public static final String ATTACHMENT_ITEM = "[data-testid='attachment-item']";
    public static final String ATTACHMENT_DOWNLOAD = "[data-testid='attachment-list:download-all'], [data-testid='attachment-item:download']";

    // ---------- Composer ----------
    // The composer root has no reliable test id, so "composer is open" means "its subject field is visible".
    public static final String COMPOSER = "[data-testid='composer:subject']";
    public static final String COMPOSER_TO = "[data-testid='composer:to']";
    public static final String COMPOSER_CC = "[data-testid='composer:cc']";
    public static final String COMPOSER_BCC = "[data-testid='composer:bcc']";
    public static final String COMPOSER_CC_BCC_TOGGLE = "[data-testid='composer:show-cc-bcc'], [data-testid='composer:cc-bcc-button']";
    public static final String COMPOSER_SUBJECT = "[data-testid='composer:subject']";
    public static final String COMPOSER_BODY_IFRAME = "[data-testid='rooster-iframe']";
    public static final String COMPOSER_SEND = "[data-testid='composer:send-button']";
    public static final String COMPOSER_CLOSE = "[data-testid='composer:close-button']";
    public static final String COMPOSER_FILE_INPUT = "[data-testid='composer:attachment-button'] ~ input[type='file'], input[type='file']";
    public static final String COMPOSER_ATTACHMENT_ITEM = "[data-testid='attachment-item']";
    public static final String COMPOSER_RECIPIENT_ERROR = "[data-testid='composer:to'] ~ .error-message, .composer-addresses-error, [data-testid='composer:to-error']";
    public static final String COMPOSER_SAVED_INDICATOR = "[data-testid='composer:header-saved'], [data-testid='composer-saved'], :text('Saved')";

    // ---------- Toasts ----------
    public static final String TOAST = ".notification, [data-testid='notification']";

    // ---------- Search ----------
    // The search bar is always visible in the header (placeholder "Search messages"); clicking it opens the advanced panel.
    public static final String SEARCH_KEYWORD = "[data-testid='search-keyword'], input[placeholder='Search messages'], #search-keyword";
    public static final String SEARCH_ADVANCED_TOGGLE = "[data-testid='search-dropdown-button'], button[title='Advanced search'], button[aria-label='Advanced search']";
    public static final String SEARCH_FROM = "[data-testid='search-from'], #search-from";
    public static final String SEARCH_FOLDER = "[data-testid='search-folder'], #search-folder";
    public static final String SEARCH_SUBMIT = "[data-testid='search-submit'], button[type='submit']:has-text('Search')";
    public static final String SEARCH_RESET = "[data-testid='search-reset'], button:has-text('Clear'), button:has-text('Reset')";
}