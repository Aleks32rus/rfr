package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Форма входа через сервер авторизации. */
public final class LoginPage {
  private final Page page;

  public LoginPage(Page page) {
    this.page = page;
  }

  @Step("Войти с данными зарегистрированного пользователя")
  public TravelsPage login(String username, String password) {
    assertThat(page).hasTitle("Login to Rangiffler");
    page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true)).fill(username);
    page.getByPlaceholder("Type your password", new Page.GetByPlaceholderOptions().setExact(true)).fill(password);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true)).click();
    return new TravelsPage(page);
  }
}
