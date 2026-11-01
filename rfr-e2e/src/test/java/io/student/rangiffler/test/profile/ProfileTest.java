package io.student.rangiffler.test.profile;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.junit.annotation.User;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.ProfilePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

@Tag("ui")
@DisplayName("Редактирование профиля")
@UsePlaywright
public class ProfileTest {
    private static final String INITIAL_COUNTRY = "Russian Federation";
    private static final String SAVED_COUNTRY = "France";
    private static final String OTHER_COUNTRY = "Canada";

    @Test
    @DisplayName("Имя сохраняется после перезагрузки страницы")
    void firstNameIsSaved(Page page, @User TestUser user) {
        login(page, user)
                .fillProfileForm("Алексей", "", INITIAL_COUNTRY)
                .clickSave()
                .reload()
                .shouldHaveFirstName("Алексей")
                .shouldHaveSurname("")
                .shouldHaveLocation(INITIAL_COUNTRY)
                .shouldHaveUsername(user.username());
    }

    @Test
    @DisplayName("Фамилия сохраняется после перезагрузки страницы")
    void surnameIsSaved(Page page, @User TestUser user) {
        login(page, user)
                .fillProfileForm("", "Ерохов", INITIAL_COUNTRY)
                .clickSave()
                .reload()
                .shouldHaveFirstName("")
                .shouldHaveSurname("Ерохов")
                .shouldHaveLocation(INITIAL_COUNTRY);
    }

    @Test
    @DisplayName("Страна меняется с России на Францию и затем на Канаду")
    void countryIsSaved(Page page, @User TestUser user) {
        login(page, user)
                .shouldHaveLocation(INITIAL_COUNTRY)
                .fillProfileForm("", "", SAVED_COUNTRY)
                .clickSave()
                .reload()
                .shouldHaveLocation(SAVED_COUNTRY)
                .fillProfileForm("", "", OTHER_COUNTRY)
                .clickSave()
                .reload()
                .shouldHaveLocation(OTHER_COUNTRY)
                .shouldHaveFirstName("")
                .shouldHaveSurname("");
    }

    @Test
    @DisplayName("Загруженный аватар сохраняется после перезагрузки страницы")
    void avatarIsSaved(Page page, @User TestUser user) throws Exception {
        Path avatar = image("avatar.svg");
        login(page, user)
                .uploadAvatar(avatar)
                .shouldHaveAvatar(avatar)
                .clickSave()
                .reload()
                .shouldHaveAvatar(avatar);
    }

    @Test
    @DisplayName("Reset возвращает сохранённые имя, фамилию, страну и аватар")
    void resetRestoresSavedProfile(Page page, @User TestUser user) throws Exception {
        Path avatar = image("avatar.svg");
        Path otherAvatar = image("other-avatar.svg");
        login(page, user)
                .fillProfileForm("Алексей", "Ерохов", SAVED_COUNTRY)
                .uploadAvatar(avatar)
                .shouldHaveAvatar(avatar)
                .clickSave()
                .reload()
                .shouldHaveFirstName("Алексей")
                .shouldHaveSurname("Ерохов")
                .shouldHaveLocation(SAVED_COUNTRY)
                .shouldHaveAvatar(avatar)
                .fillProfileForm("Иван", "Петров", OTHER_COUNTRY)
                .uploadAvatar(otherAvatar)
                .shouldHaveFirstName("Иван")
                .shouldHaveSurname("Петров")
                .shouldHaveLocation(OTHER_COUNTRY)
                .shouldHaveAvatar(otherAvatar)
                .clickReset()
                .shouldHaveFirstName("Алексей")
                .shouldHaveSurname("Ерохов")
                .shouldHaveLocation(SAVED_COUNTRY)
                .shouldHaveAvatar(avatar)
                .reload()
                .shouldHaveFirstName("Алексей")
                .shouldHaveSurname("Ерохов")
                .shouldHaveLocation(SAVED_COUNTRY)
                .shouldHaveAvatar(avatar)
                .shouldHaveUsername(user.username());
    }

    private ProfilePage login(Page page, TestUser user) {
        return new LandingPage(page)
                .open()
                .login()
                .fillLoginForm(user.username(), user.password())
                .signIn()
                .profile()
                .shouldHaveUsername(user.username());
    }

    private Path image(String name) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(ProfileTest.class.getResource("/images/" + name)).toURI());
    }
}
