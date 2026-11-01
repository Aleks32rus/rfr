package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.ApiUserRepository;
import io.student.rangiffler.data.AuthUserRepository;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Проверяет регистрацию и последующую авторизацию через интерфейс приложения.
 */
@Tag("ui")
@UsePlaywright
@DisplayName("Регистрация пользователя")
public class RegistrationTest {
    private final String username = RandomDataUtils.randomUsername();

    @AfterEach
    void deleteRegisteredUser() {
        assertAll("Очистка пользователя после регистрации",
                () -> new ApiUserRepository().delete(username),
                () -> new AuthUserRepository().delete(username));
    }

    @Test
    @DisplayName("После регистрации пользователь может войти в свой личный кабинет")
    void registeredUserCanSignIn(Page page) {
        String password = RandomDataUtils.randomPassword();
        new LandingPage(page)
                .open()
                .register()
                .fillRegistrationForm(username, password)
                .submitRegistration()
                .shouldBeRegistered()
                .signIn()
                .login()
                .fillLoginForm(username, password)
                .signIn()
                .profile()
                .shouldHaveUsername(username);
    }
}
