package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.page.LandingPage;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Проверяет регистрацию и последующую авторизацию через интерфейс приложения. */
@Tag("ui")
@UsePlaywright
@DisplayName("Регистрация пользователя")
public class RegistrationTest {

  @Test
  @DisplayName("После регистрации пользователь может войти в свой личный кабинет")
  void registeredUserCanSignIn(Page page) {
    String username = "ui_" + UUID.randomUUID().toString().replace("-", "");
    String password = UUID.randomUUID().toString().substring(0, 12);
    new LandingPage(page)
        .open(System.getenv().getOrDefault("RFR_FRONT_URL", "http://localhost:3001"))
        .register()
        .register(username, password)
        .shouldBeRegistered()
        .signIn()
        .login()
        .login(username, password)
        .shouldBeOpened()
        .profile()
        .shouldHaveUsername(username);
  }
}
