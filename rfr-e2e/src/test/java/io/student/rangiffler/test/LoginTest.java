package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.junit.annotation.User;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Проверяет вход с пользователем, созданным расширением JUnit напрямую в БД auth.
 */
@Tag("ui")
@DisplayName("Авторизация пользователя")
@UsePlaywright
public class LoginTest {

    @Test
    @DisplayName("Пользователь из БД auth может войти в личный кабинет")
    void databaseUserCanSignIn(Page page, @User TestUser user) {
        new LandingPage(page)
                .open()
                .login()
                .fillLoginForm(user.username(), user.password())
                .signIn()
                .profile()
                .shouldHaveUsername(user.username());
    }
}
