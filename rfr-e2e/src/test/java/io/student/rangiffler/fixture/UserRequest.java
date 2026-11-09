package io.student.rangiffler.fixture;

import io.student.rangiffler.junit.annotation.User;

/**
 * Преобразует описание пользователя в запрос на подготовку фикстуры.
 */
public final class UserRequest implements FixtureRequest {
    private final int friends;
    private final int invitations;

    public UserRequest(User annotation) {
        this(annotation.friends(), annotation.incomeInvitations());
    }

    public UserRequest(int friends, int invitations) {
        this.friends = friends;
        this.invitations = invitations;
    }

    @Override
    public UserFixture fixture() {
        if (friends < 0 || invitations < 0) {
            throw new IllegalArgumentException("Количество друзей и входящих приглашений не может быть отрицательным");
        }
        UserFixture fixture = new SingleUserFixture();
        if (friends > 0) {
            fixture = new UserWithFriendsFixture(fixture, friends);
        }
        if (invitations > 0) {
            fixture = new UserWithIncomeInvitations(fixture, invitations);
        }
        return fixture;
    }
}
