package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.qameta.allure.Param;
import io.student.rangiffler.config.TestConfig;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Просмотр и редактирование профиля текущего пользователя.
 */
public final class ProfilePage {
    public static String URL = TestConfig.FRONT_URL + "/profile";

    private final Page page;
    private final Locator username;
    private final Locator firstName;
    private final Locator surname;
    private final Locator location;
    private final Locator avatarInput;
    private final Locator avatar;
    private final Locator save;
    private final Locator reset;

    public ProfilePage(Page page) {
        this.page = page;
        this.username = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true));
        this.firstName = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("First Name").setExact(true));
        this.surname = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Surname").setExact(true));
        this.location = page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Location").setExact(true));
        this.avatarInput = page.getByRole(AriaRole.MAIN).locator("input[type='file']");
        this.avatar = page.getByRole(AriaRole.MAIN).locator("img:not([alt])[src^='data:image/']");
        this.save = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true));
        this.reset = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset").setExact(true));
    }

    @Step("Открыть профиль текущего пользователя")
    public ProfilePage open() {
        page.navigate(URL);
        return this;
    }

    @Step("Заполнить форму профиля")
    public ProfilePage fillProfileForm(@Param("Имя") String firstName,
                                       @Param("Фамилия") String surname,
                                       @Param("Страна") String country) {
        this.firstName.fill(firstName);
        this.surname.fill(surname);
        location.click();
        page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions()
                .setHas(page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(country).setExact(true)))).click();
        return this;
    }

    @Step("Загрузить аватар профиля")
    public ProfilePage uploadAvatar(@Param("Файл аватара") Path file) {
        avatarInput.setInputFiles(file);
        return this;
    }

    @Step("Сохранить изменения профиля")
    public ProfilePage clickSave() {
        page.waitForResponse(response -> {
            String postData = response.request().postData();
            return response.request().method().equals("POST")
                    && response.url().endsWith("/graphql")
                    && postData != null
                    && Pattern.compile("\"operationName\"\\s*:\\s*\"UpdateUser\"").matcher(postData).find();
        }, save::click).finished();
        return this;
    }

    @Step("Сбросить несохранённые изменения профиля")
    public ProfilePage clickReset() {
        reset.click();
        return this;
    }

    @Step("Перезагрузить страницу профиля")
    public ProfilePage reload() {
        page.reload();
        return this;
    }

    @Step("Проверить имя в профиле")
    public ProfilePage shouldHaveFirstName(@Param("Ожидаемое имя") String firstName) {
        assertThat(this.firstName).hasValue(firstName);
        return this;
    }

    @Step("Проверить фамилию в профиле")
    public ProfilePage shouldHaveSurname(@Param("Ожидаемая фамилия") String surname) {
        assertThat(this.surname).hasValue(surname);
        return this;
    }

    @Step("Проверить страну в профиле")
    public ProfilePage shouldHaveLocation(@Param("Ожидаемая страна") String country) {
        assertThat(location).containsText(country);
        assertThat(location.getByRole(AriaRole.IMG, new Locator.GetByRoleOptions().setName(country).setExact(true))).isVisible();
        return this;
    }

    @Step("Проверить наличие аватара в профиле")
    public ProfilePage shouldHaveAvatar() {
        assertThat(avatar).isVisible();
        assertThat(avatar).hasAttribute("src", Pattern.compile("^data:image/.+"));
        return this;
    }

    @Step("Проверить, что аватар соответствует файлу")
    public ProfilePage shouldHaveAvatar(@Param("Ожидаемый файл аватара") Path file) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
        assertThat(avatar).isVisible();
        assertThat(avatar).hasAttribute("src", Pattern.compile("^data:image/[^;]+;base64," + base64.replace("+", "\\+") + "$"));
        return this;
    }

    @Step("Проверить, что выполнен вход в созданный аккаунт")
    public ProfilePage shouldHaveUsername(@Param("Ожидаемый логин") String username) {
        assertThat(this.username).hasValue(username);
        return this;
    }
}
