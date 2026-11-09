package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.junit.annotation.User;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.PeoplePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Проверяет отображение созданных декораторами прекондишенов через интерфейс.
 */
@Tag("ui")
@UsePlaywright
@DisplayName("Прекондишены дружбы в интерфейсе")
final class FriendshipPreconditionsTest {
    @Test
    @DisplayName("Пользователь видит заданное количество друзей")
    void preparedFriendsCannotDisappearFromPeople(Page page, @User(friends = 3, incomeInvitations = 2) TestUser user) {
        new LandingPage(page).open().login().fillLoginForm(user.username(), user.password()).signIn();
        new PeoplePage(page).open().friends().shouldHaveFriends(3);
    }

    @Test
    @DisplayName("Пользователь видит заданное количество входящих приглашений")
    void preparedIncomeInvitationsCannotDisappearFromPeople(Page page,
                                                            @User(friends = 3, incomeInvitations = 2) TestUser user) {
        new LandingPage(page).open().login().fillLoginForm(user.username(), user.password()).signIn();
        new PeoplePage(page).open().incomeInvitations().shouldHaveIncomeInvitations(2);
    }
}
