package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Форма входа через сервер авторизации.
 */
public final class LoginPage {
    public static String URL = TestConfig.AUTH_URL + "/login";

    private final Page page;
    private final Locator username;
    private final Locator password;
    private final Locator submit;
    private final Locator error;

    public LoginPage(Page page) {
        this.page = page;
        this.username = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true));
        this.password = page.getByPlaceholder("Type your password", new Page.GetByPlaceholderOptions().setExact(true));
        this.submit = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
        this.error = page.locator(".form__error");
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

    @Step("Отправить форму входа с некорректными учётными данными")
    public LoginPage signInWithInvalidCredentials() {
        submit.click();
        return this;
    }

    @Step("Проверить сообщение о неверных учётных данных")
    public LoginPage shouldHaveBadCredentialsError() {
        assertThat(error).isVisible();
        assertThat(error).hasText("Bad credentials");
        assertThat(submit).isVisible();
        return this;
    }
}
