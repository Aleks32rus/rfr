package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Форма регистрации и подтверждение создания аккаунта. */
public final class RegistrationPage {
  private final Page page;

  public RegistrationPage(Page page) {
    this.page = page;
  }

  @Step("Зарегистрировать нового пользователя")
  public RegistrationPage register(String username, String password) {
    page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true)).fill(username);
    page.getByPlaceholder("Type your password", new Page.GetByPlaceholderOptions().setExact(true)).fill(password);
    page.getByPlaceholder("Repeat your password", new Page.GetByPlaceholderOptions().setExact(true)).fill(password);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up").setExact(true)).click();
    return this;
  }

  @Step("Проверить успешную регистрацию")
  public RegistrationPage shouldBeRegistered() {
    assertThat(page.getByText("Congratulations! You've registered!", new Page.GetByTextOptions().setExact(true))).isVisible();
    return this;
  }

  @Step("Вернуться на стартовую страницу для входа")
  public LandingPage signIn() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true)).click();
    return new LandingPage(page);
  }
}
