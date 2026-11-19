package io.student.rangiffler.test.people;

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
 * Проверяет состав, доступные кнопки и пустое состояние вкладки People.
 */
@Tag("ui")
@UsePlaywright
@DisplayName("Вкладка исходящих приглашений")
final class OutcomeInvitationsTabTest {
    @Test
    @DisplayName("Исходящее приглашение помечено как ожидающее принятия")
    void invitationCannotOmitWaitingStatus(Page page, @User(outcomeInvitations = 1) TestUser user) {
        new LandingPage(page).open().login().fillLoginForm(user.username(), user.password()).signIn();
        new PeoplePage(page).open().outcomeInvitations()
                .shouldHaveWaitingInvitation(user.outcomeInvitations().getFirst().username());
    }

    @Test
    @DisplayName("Вкладка содержит только связанную запись текущего пользователя")
    void tabCannotIncludeAnotherUsersRelationships(Page page, @User(outcomeInvitations = 1) TestUser user,
                                                   @User(outcomeInvitations = 1) TestUser stranger) {
        new LandingPage(page).open().login().fillLoginForm(user.username(), user.password()).signIn();
        new PeoplePage(page).open().outcomeInvitations().shouldHavePeople(user.outcomeInvitations().getFirst().username());
    }

    @Test
    @DisplayName("Строка пользователя содержит предусмотренные кнопки")
    void rowCannotOmitExpectedActions(Page page, @User(outcomeInvitations = 1) TestUser user) {
        new LandingPage(page).open().login().fillLoginForm(user.username(), user.password()).signIn();
        new PeoplePage(page).open().outcomeInvitations()
                .shouldHaveActions(user.outcomeInvitations().getFirst().username(), "Cancel");
    }

    @Test
    @DisplayName("Без связей отображается пустое состояние")
    void emptyTabCannotDisplayPeople(Page page, @User TestUser user) {
        new LandingPage(page).open().login().fillLoginForm(user.username(), user.password()).signIn();
        new PeoplePage(page).open().outcomeInvitations().shouldBeEmpty();
    }
}
