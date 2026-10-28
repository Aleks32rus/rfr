package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.ProfilePage;

/**
 * Общая шапка страниц личного кабинета.
 */
public final class HeaderComponent {
    private final Page page;
    private final Locator root;
    private final Locator profile;

    public HeaderComponent(Page page) {
        this.page = page;
        this.root = page.getByRole(AriaRole.BANNER);
        this.profile = root.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile").setExact(true));
    }

    @Step("Перейти в профиль через шапку")
    public ProfilePage profile() {
        profile.click();
        return new ProfilePage(page);
    }
}
