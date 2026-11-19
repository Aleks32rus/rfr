package io.student.rangiffler.page;

/**
 * Описывает действия и проверки страницы пользователей.
 */
public interface PeopleScreen {
    PeoplePage open();

    PeoplePage friends();

    PeoplePage allPeople();

    PeoplePage incomeInvitations();

    PeoplePage outcomeInvitations();

    PeoplePage fillSearch(String username);

    PeoplePage submitSearch();

    PeoplePage sendInvitation(String username);

    PeoplePage acceptInvitation(String username);

    LandingPage logout();

    PeoplePage shouldHaveFriends(int count);

    PeoplePage shouldHaveIncomeInvitations(int count);

    PeoplePage shouldHaveOutgoingInvitation(String username);

    PeoplePage shouldHaveFriend(String username);

    PeoplePage shouldHavePeople(String... usernames);

    PeoplePage shouldHaveActions(String username, String... actions);

    PeoplePage shouldHaveWaitingInvitation(String username);

    PeoplePage shouldBeEmpty();
}
