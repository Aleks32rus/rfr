package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import java.util.Optional;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Управляет вкладками пользователей, поиском и действиями со связями.
 */
public final class PeoplePage implements PeopleScreen {
    private final Page page;
    private final Locator root;
    private final Locator tabs;
    private final String query;

    public PeoplePage(Page page) {
        this(page, "GetFriends");
    }

    public PeoplePage(Page page, String query) {
        this.page = page;
        this.root = page.getByRole(AriaRole.MAIN);
        this.tabs = root.getByRole(AriaRole.TABLIST, new Locator.GetByRoleOptions().setName("People tabs").setExact(true));
        this.query = query;
    }

    @Override
    @Step("Открыть страницу пользователей через меню")
    public PeoplePage open() {
        page.waitForResponse(response -> operation(response, "GetFriends"),
                () -> page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("People").setExact(true))
                        .click()).finished();
        return new PeoplePage(page);
    }

    @Override
    @Step("Открыть вкладку друзей")
    public PeoplePage friends() {
        return selectTab("Friends", "GetFriends");
    }

    @Override
    @Step("Открыть вкладку всех пользователей")
    public PeoplePage allPeople() {
        return selectTab("All People", "GetPeople");
    }

    @Override
    @Step("Открыть вкладку входящих приглашений")
    public PeoplePage incomeInvitations() {
        return selectTab("Income invitations", "GetInvitations");
    }

    @Override
    @Step("Открыть вкладку исходящих приглашений")
    public PeoplePage outcomeInvitations() {
        return selectTab("Outcome invitations", "GetOutcomeInvitations");
    }

    @Override
    @Step("Заполнить поиск пользователя: {username}")
    public PeoplePage fillSearch(String username) {
        root.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("search people").setExact(true))
                .fill(username);
        return this;
    }

    @Override
    @Step("Выполнить поиск пользователей")
    public PeoplePage submitSearch() {
        page.waitForResponse(response -> operation(response, query),
                () -> root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("search").setExact(true))
                        .click()).finished();
        return this;
    }

    @Override
    @Step("Отправить приглашение пользователю: {username}")
    public PeoplePage sendInvitation(String username) {
        page.waitForResponse(response -> operation(response, "FriendshipAction"),
                () -> row(username).getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Add").setExact(true)).click()).finished();
        return this;
    }

    @Override
    @Step("Принять приглашение пользователя: {username}")
    public PeoplePage acceptInvitation(String username) {
        page.waitForResponse(response -> operation(response, "FriendshipAction"),
                () -> row(username).getByRole(AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Accept").setExact(true)).click()).finished();
        return this;
    }

    @Override
    @Step("Выйти из аккаунта")
    public LandingPage logout() {
        page.getByRole(AriaRole.BANNER).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Logout").setExact(true)).click();
        return new LandingPage(page);
    }

    @Override
    @Step("Проверить количество друзей: {count}")
    public PeoplePage shouldHaveFriends(int count) {
        assertThat(root.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHas(
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove").setExact(true)))))
                .hasCount(count);
        return this;
    }

    @Override
    @Step("Проверить количество входящих приглашений: {count}")
    public PeoplePage shouldHaveIncomeInvitations(int count) {
        assertThat(root.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHas(
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Accept").setExact(true)))))
                .hasCount(count);
        return this;
    }

    @Override
    @Step("Проверить исходящее приглашение пользователю: {username}")
    public PeoplePage shouldHaveOutgoingInvitation(String username) {
        assertThat(row(username).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Cancel").setExact(true))).isVisible();
        return this;
    }

    @Override
    @Step("Проверить дружбу с пользователем: {username}")
    public PeoplePage shouldHaveFriend(String username) {
        assertThat(row(username).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Remove").setExact(true))).isVisible();
        return this;
    }

    @Override
    @Step("Проверить точный состав пользователей вкладки: {usernames}")
    public PeoplePage shouldHavePeople(String... usernames) {
        assertThat(root.getByRole(AriaRole.TABLE).locator("tbody tr > td:nth-child(2)"))
                .hasText(usernames);
        return this;
    }

    @Override
    @Step("Проверить кнопки строки пользователя {username}: {actions}")
    public PeoplePage shouldHaveActions(String username, String... actions) {
        assertThat(row(username).getByRole(AriaRole.BUTTON)).hasText(actions);
        return this;
    }

    @Override
    @Step("Проверить ожидание принятия приглашения пользователем: {username}")
    public PeoplePage shouldHaveWaitingInvitation(String username) {
        assertThat(row(username).getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).isVisible();
        return this;
    }

    @Override
    @Step("Проверить пустое состояние вкладки пользователей")
    public PeoplePage shouldBeEmpty() {
        assertThat(root.getByRole(AriaRole.ALERT).or(root.getByText("There are no users yet",
                new Locator.GetByTextOptions().setExact(true)))).hasText(new String[]{"There are no users yet"});
        return this;
    }

    private PeoplePage selectTab(String name, String operation) {
        Locator tab = tabs.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName(name).setExact(true));
        if (!"true".equals(tab.getAttribute("aria-selected"))) {
            page.waitForResponse(response -> operation(response, operation), tab::click).finished();
        }
        return new PeoplePage(page, operation);
    }

    private Locator row(String username) {
        return root.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHas(
                page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(username).setExact(true))));
    }

    private boolean operation(Response response, String operation) {
        return response.request().method().equals("POST") && response.url().endsWith("/graphql")
                && Pattern.compile("\"operationName\"\\s*:\\s*\"" + Pattern.quote(operation) + "\"")
                .matcher(Optional.ofNullable(response.request().postData()).orElse("")).find();
    }
}
