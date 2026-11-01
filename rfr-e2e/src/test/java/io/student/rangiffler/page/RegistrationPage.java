package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Форма регистрации и подтверждение создания аккаунта.
 */
public final class RegistrationPage {
    public static String URL = TestConfig.AUTH_URL + "/register";

    private final Page page;
    private final Locator username;
    private final Locator password;
    private final Locator confirmation;
    private final Locator submit;
    private final Locator success;
    private final Locator signIn;

    public RegistrationPage(Page page) {
        this.page = page;
        this.username = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true));
        this.password = page.getByPlaceholder("Type your password", new Page.GetByPlaceholderOptions().setExact(true));
        this.confirmation = page.getByPlaceholder("Repeat your password", new Page.GetByPlaceholderOptions().setExact(true));
        this.submit = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up").setExact(true));
        this.success = page.getByText("Congratulations! You've registered!", new Page.GetByTextOptions().setExact(true));
        this.signIn = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
    }

    @Step("Заполнить форму регистрации")
    public RegistrationPage fillRegistrationForm(@Param("Логин") String username,
                                                 @Param(name = "Пароль", mode = Parameter.Mode.MASKED) String password) {
        this.username.fill(username);
        this.password.fill(password);
        confirmation.fill(password);
        return this;
    }

    @Step("Отправить форму регистрации")
    public RegistrationPage submitRegistration() {
        submit.click();
        return this;
    }

    @Step("Проверить успешную регистрацию")
    public RegistrationPage shouldBeRegistered() {
        assertThat(success).isVisible();
        return this;
    }

    @Step("Вернуться на стартовую страницу для входа")
    public LandingPage signIn() {
        signIn.click();
        return new LandingPage(page);
    }
}
