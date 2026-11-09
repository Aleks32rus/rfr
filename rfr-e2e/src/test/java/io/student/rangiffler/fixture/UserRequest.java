package io.student.rangiffler.fixture;

import io.student.rangiffler.junit.annotation.User;

/**
 * Преобразует описание пользователя в запрос на подготовку фикстуры.
 */
public final class UserRequest implements FixtureRequest {
    private final int friends;
    private final int invitations;
    private final int outcome;

    public UserRequest(User user) {
        this(user.friends(), user.incomeInvitations(), user.outcomeInvitations());
    }

    public UserRequest(int friends, int invitations) {
        this(friends, invitations, 0);
    }

    public UserRequest(int friends, int invitations, int outcome) {
        this.friends = friends;
        this.invitations = invitations;
        this.outcome = outcome;
    }

    @Override
    public UserFixture fixture() {
        if (friends < 0 || invitations < 0) {
            throw new IllegalArgumentException("Количество друзей и входящих приглашений не может быть отрицательным");
        }
        if (outcome < 0) {
            throw new IllegalArgumentException("Количество исходящих приглашений не может быть отрицательным");
        }
        UserFixture fixture = new SingleUserFixture();
        if (friends > 0) {
            fixture = new UserWithFriendsFixture(fixture, friends);
        }
        if (invitations > 0) {
            fixture = new UserWithIncomeInvitations(fixture, invitations);
        }
        if (outcome > 0) {
            fixture = new UserWithOutcomeInvitationsFixture(fixture, outcome);
        }
        return fixture;
    }
}
