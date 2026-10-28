package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.qameta.allure.Param;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Профиль для проверки личности авторизованного пользователя.
 */
public final class ProfilePage {
    public static String URL = TestConfig.FRONT_URL + "/profile";

    private final Page page;
    private final Locator username;

    public ProfilePage(Page page) {
        this.page = page;
        this.username = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true));
    }

    @Step("Проверить, что выполнен вход в созданный аккаунт")
    public ProfilePage shouldHaveUsername(@Param("Ожидаемый логин") String username) {
        assertThat(this.username).hasValue(username);
        return this;
    }
}
