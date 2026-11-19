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
 * Проверяет отправку и принятие приглашений через интерфейс приложения.
 */
@Tag("ui")
@UsePlaywright
@DisplayName("Действия с дружбой в интерфейсе")
final class FriendshipActionsTest {
    @Test
    @DisplayName("Пользователь может отправить приглашение через интерфейс")
    void sentInvitationCannotDisappearFromOutcome(Page page, @User TestUser user, @User TestUser peer) {
        new LandingPage(page).open().login().fillLoginForm(user.username(), user.password()).signIn();
        new PeoplePage(page).open().allPeople().fillSearch(peer.username()).submitSearch()
                .sendInvitation(peer.username()).outcomeInvitations()
                .shouldHaveOutgoingInvitation(peer.username());
    }

    @Test
    @DisplayName("Пользователь может принять приглашение через интерфейс")
    void acceptedFriendshipCannotDisappearFromFriends(Page page, @User TestUser user, @User TestUser peer) {
        new LandingPage(page).open().login().fillLoginForm(user.username(), user.password()).signIn();
        new PeoplePage(page).open().allPeople().fillSearch(peer.username()).submitSearch()
                .sendInvitation(peer.username()).logout().login()
                .fillLoginForm(peer.username(), peer.password()).signIn();
        new PeoplePage(page).open().incomeInvitations().fillSearch(user.username()).submitSearch()
                .acceptInvitation(user.username()).friends().shouldHaveFriend(user.username());
    }
}
