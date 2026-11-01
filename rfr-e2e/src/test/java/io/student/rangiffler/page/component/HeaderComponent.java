package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.ProfilePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Общая шапка страниц личного кабинета.
 */
public final class HeaderComponent {
    private final Page page;
    private final Locator root;
    private final Locator profile;
    private final Locator logo;
    private final Locator menu;
    private final Locator theme;
    private final Locator logout;

    public HeaderComponent(Page page) {
        this.page = page;
        this.root = page.getByRole(AriaRole.BANNER);
        this.profile = root.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile").setExact(true));
        this.logo = root.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Rangiffler").setExact(true));
        this.menu = root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("open drawer").setExact(true));
        this.theme = root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Toggle dark mode").setExact(true));
        this.logout = root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logout").setExact(true));
    }

    @Step("Проверить логотип и элементы управления в шапке")
    public HeaderComponent shouldHaveMainControls() {
        assertThat(logo).isVisible();
        assertThat(logo).hasAttribute("href", "/my-travels");
        assertThat(menu).isVisible();
        assertThat(menu).isEnabled();
        assertThat(theme).isVisible();
        assertThat(theme).isEnabled();
        assertThat(profile).isVisible();
        assertThat(profile).hasAttribute("href", "/profile");
        assertThat(logout).isVisible();
        assertThat(logout).isEnabled();
        return this;
    }

    @Step("Перейти в профиль через шапку")
    public ProfilePage profile() {
        profile.click();
        return new ProfilePage(page);
    }
}
