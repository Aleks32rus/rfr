package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.junit.annotation.User;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
@UsePlaywright
@DisplayName("Главная страница")
public class MainPageTest {

    @Test
    @DisplayName("После входа отображаются основные элементы главной страницы")
    void mainElementsAreVisible(Page page, @User TestUser user) {
        new LandingPage(page)
                .open()
                .login()
                .fillLoginForm(user.username(), user.password())
                .signIn()
                .shouldHaveHeader()
                .shouldHaveNavigation()
                .shouldHaveTravelsMap()
                .shouldHaveTravelsFilters()
                .shouldHavePhotosSection();
    }
}
