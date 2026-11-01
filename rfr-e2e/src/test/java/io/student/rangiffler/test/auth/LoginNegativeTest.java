package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.junit.annotation.User;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
@DisplayName("Вход с некорректными учётными данными")
@UsePlaywright(LoginNegativeTest.EnglishLocale.class)
public class LoginNegativeTest {

    public static final class EnglishLocale implements OptionsFactory {
        @Override
        public Options getOptions() {
            return new Options().setContextOptions(new Browser.NewContextOptions().setLocale("en-US"));
        }
    }

    @Test
    @DisplayName("Неверный пароль существующего пользователя: Bad credentials")
    void wrongPasswordShowsBadCredentials(Page page, @User TestUser user) {
        new LandingPage(page)
                .open()
                .login()
                .fillLoginForm(user.username(), user.password() + "!")
                .signInWithInvalidCredentials()
                .shouldHaveBadCredentialsError();
    }

    @Test
    @DisplayName("Несуществующий пользователь: Bad credentials")
    void unknownUsernameShowsBadCredentials(Page page) {
        new LandingPage(page)
                .open()
                .login()
                .fillLoginForm("missing_" + RandomDataUtils.randomUsername(), RandomDataUtils.randomPassword())
                .signInWithInvalidCredentials()
                .shouldHaveBadCredentialsError();
    }
}
