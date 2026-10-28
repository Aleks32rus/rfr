package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.HeaderComponent;

/**
 * Страница путешествий, доступная после авторизации.
 */
public final class TravelsPage {
    public static String URL = TestConfig.FRONT_URL + "/my-travels";

    private final Page page;
    private final HeaderComponent header;

    public TravelsPage(Page page) {
        this.page = page;
        this.header = new HeaderComponent(page);
    }

    @Step("Открыть профиль текущего пользователя")
    public ProfilePage profile() {
        return header.profile();
    }
}
