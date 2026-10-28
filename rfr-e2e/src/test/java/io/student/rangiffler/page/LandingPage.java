package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/** Стартовая страница с переходами к регистрации и входу. */
public final class LandingPage {
  private final Page page;

  public LandingPage(Page page) {
    this.page = page;
  }

  @Step("Открыть приложение")
  public LandingPage open(String url) {
    page.navigate(url);
    return this;
  }

  @Step("Перейти к регистрации")
  public RegistrationPage register() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register").setExact(true)).click();
    return new RegistrationPage(page);
  }

  @Step("Перейти к входу")
  public LoginPage login() {
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login").setExact(true)).click();
    return new LoginPage(page);
  }
}
