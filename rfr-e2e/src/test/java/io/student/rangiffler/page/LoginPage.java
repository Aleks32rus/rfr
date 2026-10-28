package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import io.student.rangiffler.config.TestConfig;

/**
 * Форма входа через сервер авторизации.
 */
public final class LoginPage {
    public static String URL = TestConfig.AUTH_URL + "/login";

    private final Page page;
    private final Locator username;
    private final Locator password;
    private final Locator submit;

    public LoginPage(Page page) {
        this.page = page;
        this.username = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true));
        this.password = page.getByPlaceholder("Type your password", new Page.GetByPlaceholderOptions().setExact(true));
        this.submit = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
    }

    @Step("Заполнить форму входа")
    public LoginPage fillLoginForm(@Param("Логин") String username,
                                   @Param(name = "Пароль", mode = Parameter.Mode.MASKED) String password) {
        this.username.fill(username);
        this.password.fill(password);
        return this;
    }

    @Step("Отправить форму входа")
    public TravelsPage signIn() {
        submit.click();
        return new TravelsPage(page);
    }
}
