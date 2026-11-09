package io.student.rangiffler.test;

import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Проверяет ограничения запроса и совместную подготовку двух видов связей.
 */
@DisplayName("Запрос подготовки пользователя и его связей")
final class UserRequestTest {
    @Test
    @DisplayName("Запрос отклоняет отрицательное количество друзей")
    void requestCannotHaveNegativeFriends() {
        assertThrows(IllegalArgumentException.class, () -> new UserRequest(-7, 3).fixture(),
                "Запрос не должен допускать отрицательное количество друзей");
    }

    @Test
    @DisplayName("Запрос отклоняет отрицательное количество входящих приглашений")
    void requestCannotHaveNegativeIncomeInvitations() {
        assertThrows(IllegalArgumentException.class, () -> new UserRequest(5, -9).fixture(),
                "Запрос не должен допускать отрицательное количество входящих приглашений");
    }

    @Test
    @Tag("db")
    @DisplayName("Запрос создаёт друзей и входящие приглашения одновременно")
    void requestCannotLoseEitherDecorator() {
        try (UserFixture fixture = new UserRequest(3, 5).fixture()) {
            Friendships friendships = new Friendships(fixture.create());
            fixture.create();
            assertEquals(List.of(3L, 5L, 0L), List.of(friendships.count(FriendshipStatus.ACCEPTED),
                            friendships.incomeInvitations(), friendships.outcomeInvitations()),
                    "Запрос не должен терять друзей или менять направление приглашений");
        }
    }
}
