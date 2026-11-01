package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Проверяет регистрацию и последующую авторизацию через интерфейс приложения.
 */
@Tag("ui")
@UsePlaywright
@DisplayName("Регистрация пользователя")
public class RegistrationTest {
    @Test
    @DisplayName("После регистрации пользователь может войти в свой личный кабинет")
    void registeredUserCanSignIn(Page page) {
        try (UserFixture fixture = new UserFixture()) {
            TestUser user = fixture.user();
            new LandingPage(page)
                    .open()
                    .register()
                    .fillRegistrationForm(user.username(), user.password())
                    .submitRegistration()
                    .shouldBeRegistered()
                    .signIn()
                    .login()
                    .fillLoginForm(user.username(), user.password())
                    .signIn()
                    .profile()
                    .shouldHaveUsername(user.username());
        }
    }
}
