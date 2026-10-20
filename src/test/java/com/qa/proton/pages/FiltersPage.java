package com.qa.proton.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.qa.proton.config.Config;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Settings > Filters. Uses role/label locators (more resilient than CSS) because the modal has few test ids.
 * Free Proton plans allow a limited number of custom filters, so tests always clean up in finally blocks.
 */
public class FiltersPage {
    private final Page page;

    public FiltersPage(Page page) {
        this.page = page;
    }

    public FiltersPage open() {
        page.navigate(Config.SETTINGS_URL);
        assertThat(button("Add filter")).isVisible();
        return this;
    }

    private Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)).first();
    }

    public FiltersPage createSubjectFilterArchive(String filterName, String subjectKeyword) {
        button("Add filter").click();
        page.getByLabel("Filter name").fill(filterName);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
        page.getByPlaceholder("Subject").first().fill(subjectKeyword);   // condition: Subject contains <keyword>
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
        page.getByText("Archive").first().click();                         // action: move to Archive
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
        assertThat(filterRow(filterName)).isVisible();
        return this;
    }

    public Locator filterRow(String filterName) {
        return page.locator("tr, [role='row']").filter(new Locator.FilterOptions().setHasText(filterName)).first();
    }

    public FiltersPage deleteFilter(String filterName) {
        filterRow(filterName).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete")).last().click();
        assertThat(filterRow(filterName)).isHidden();
        return this;
    }
}
