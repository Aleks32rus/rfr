package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

/**
 * Стартовая страница с переходами к регистрации и входу.
 */
public final class LandingPage {
    public static String URL = TestConfig.FRONT_URL;

    private final Page page;
    private final Locator registration;
    private final Locator login;

    public LandingPage(Page page) {
        this.page = page;
        this.registration = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register").setExact(true));
        this.login = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login").setExact(true));
    }

    @Step("Открыть приложение")
    public LandingPage open() {
        page.navigate(URL);
        return this;
    }

    @Step("Перейти к регистрации")
    public RegistrationPage register() {
        registration.click();
        return new RegistrationPage(page);
    }

    @Step("Перейти к входу")
    public LoginPage login() {
        login.click();
        return new LoginPage(page);
    }
}
