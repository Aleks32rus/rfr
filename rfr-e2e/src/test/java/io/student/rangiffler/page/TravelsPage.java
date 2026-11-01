package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.HeaderComponent;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Страница путешествий, доступная после авторизации.
 */
public final class TravelsPage {
    public static String URL = TestConfig.FRONT_URL + "/my-travels";

    private final Page page;
    private final HeaderComponent header;
    private final Locator navigation;
    private final Locator mapHeading;
    private final Locator worldMap;
    private final Locator collapseMap;
    private final Locator onlyMyTravels;
    private final Locator withFriends;
    private final Locator photosHeading;
    private final Locator addPhoto;

    public TravelsPage(Page page) {
        this.page = page;
        this.header = new HeaderComponent(page);
        this.navigation = page.getByRole(AriaRole.LIST).filter(new Locator.FilterOptions()
                .setHas(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("My map").setExact(true))));
        Locator main = page.getByRole(AriaRole.MAIN);
        this.mapHeading = main.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Travels map").setExact(true));
        this.worldMap = main.getByRole(AriaRole.IMG, new Locator.GetByRoleOptions().setName("World map").setExact(true));
        this.collapseMap = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Collapse map").setExact(true));
        Locator filter = main.getByRole(AriaRole.GROUP, new Locator.GetByRoleOptions().setName("Travels filter").setExact(true));
        this.onlyMyTravels = filter.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Only my travels").setExact(true));
        this.withFriends = filter.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("With friends").setExact(true));
        this.photosHeading = main.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Photos").setExact(true));
        this.addPhoto = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add photo").setExact(true));
    }

    @Step("Проверить шапку главной страницы")
    public TravelsPage shouldHaveHeader() {
        header.shouldHaveMainControls();
        return this;
    }

    @Step("Проверить ссылки основного меню")
    public TravelsPage shouldHaveNavigation() {
        assertThat(navigation.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("My map").setExact(true)))
                .isVisible();
        assertThat(navigation.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("My map").setExact(true)))
                .hasAttribute("href", "/my-travels");
        assertThat(navigation.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("People").setExact(true)))
                .isVisible();
        assertThat(navigation.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("People").setExact(true)))
                .hasAttribute("href", "/people");
        assertThat(navigation.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile").setExact(true)))
                .isVisible();
        assertThat(navigation.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile").setExact(true)))
                .hasAttribute("href", "/profile");
        return this;
    }

    @Step("Проверить карту путешествий на главной странице")
    public TravelsPage shouldHaveTravelsMap() {
        assertThat(page).hasURL(URL);
        assertThat(mapHeading).isVisible();
        assertThat(worldMap).isVisible();
        assertThat(collapseMap).isVisible();
        assertThat(collapseMap).isEnabled();
        assertThat(collapseMap).hasAttribute("aria-expanded", "true");
        return this;
    }

    @Step("Проверить фильтры путешествий и выбранный режим по умолчанию")
    public TravelsPage shouldHaveTravelsFilters() {
        assertThat(onlyMyTravels).isVisible();
        assertThat(onlyMyTravels).isEnabled();
        assertThat(onlyMyTravels).hasAttribute("aria-pressed", "true");
        assertThat(withFriends).isVisible();
        assertThat(withFriends).isEnabled();
        assertThat(withFriends).hasAttribute("aria-pressed", "false");
        return this;
    }

    @Step("Проверить раздел фотографий и кнопку добавления фото")
    public TravelsPage shouldHavePhotosSection() {
        assertThat(photosHeading).isVisible();
        assertThat(addPhoto).isVisible();
        assertThat(addPhoto).isEnabled();
        return this;
    }

    @Step("Открыть профиль текущего пользователя")
    public ProfilePage profile() {
        return header.profile();
    }
}
