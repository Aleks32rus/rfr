package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserWithFriendsFixture;
import io.student.rangiffler.fixture.UserWithIncomeInvitations;
import io.student.rangiffler.test.fake.FailingUserFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Проверяет подготовку связей и очистку всех участников декоратора.
 */
final class UserWithFriendsFixtureTest {
    @Test
    @Tag("db")
    void preparedUserCannotLoseFriends() {
        List<UserFixture> friends = Stream.<UserFixture>generate(SingleUserFixture::new).limit(5).toList();
        try (UserFixture fixture = new UserWithFriendsFixture(new SingleUserFixture(), friends)) {
            TestUser user = fixture.create();
            fixture.create();
            assertEquals(List.of(friends.stream().map(UserFixture::user).toList(), user.friends()),
                    List.of(user.friends(), fixture.user().friends()),
                    "Подготовка пользователя не должна терять или дублировать друзей");
        }
    }

    @Test
    @Tag("db")
    void invitationsCannotDiscardPreparedFriends() {
        List<UserFixture> friends = Stream.<UserFixture>generate(SingleUserFixture::new).limit(3).toList();
        List<UserFixture> invitations = Stream.<UserFixture>generate(SingleUserFixture::new).limit(2).toList();
        try (UserFixture fixture = new UserWithIncomeInvitations(
                new UserWithFriendsFixture(new SingleUserFixture(), friends), invitations)) {
            TestUser user = fixture.create();
            assertEquals(List.of(friends.stream().map(UserFixture::user).toList(),
                            invitations.stream().map(UserFixture::user).toList()),
                    List.of(user.friends(), user.incomeInvitations()),
                    "Добавление приглашений не должно терять друзей или отправителей");
        }
    }

    @Test
    @Tag("db")
    void friendsCannotDiscardPreparedInvitations() {
        List<UserFixture> friends = Stream.<UserFixture>generate(SingleUserFixture::new).limit(2).toList();
        List<UserFixture> invitations = Stream.<UserFixture>generate(SingleUserFixture::new).limit(4).toList();
        try (UserFixture fixture = new UserWithFriendsFixture(
                new UserWithIncomeInvitations(new SingleUserFixture(), invitations), friends)) {
            TestUser user = fixture.create();
            assertEquals(List.of(friends.stream().map(UserFixture::user).toList(),
                            invitations.stream().map(UserFixture::user).toList()),
                    List.of(user.friends(), user.incomeInvitations()),
                    "Добавление друзей не должно терять отправителей или друзей");
        }
    }

    @Test
    @Tag("db")
    void repeatedCreationCannotDuplicateFriends() {
        try (UserFixture fixture = new UserWithFriendsFixture(new SingleUserFixture(), 3)) {
            Friendships friendships = new Friendships(fixture.create());
            fixture.create();
            assertEquals(3L, friendships.count(FriendshipStatus.ACCEPTED),
                    "Повторная подготовка не должна изменять заданное количество друзей");
        }
    }

    @Test
    @Tag("db")
    void closingCannotLeaveParticipantsInEitherDatabase() {
        UserFixture owner = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "owner_" + UUID.randomUUID(), "G8p3J6n2D9r4"));
        List<UserFixture> friends = Stream.<UserFixture>generate(SingleUserFixture::new).limit(5).toList();
        try (UserFixture fixture = new UserWithFriendsFixture(owner, friends)) {
            fixture.create();
        }
        assertFalse(Stream.concat(friends.stream(), Stream.of(owner))
                        .anyMatch(fixture -> new Profile(fixture.user()).exist() || new AuthAccount(fixture.user()).exist()),
                "После закрытия участники декоратора не должны оставаться в БД");
    }

    @Test
    void failedCreationCannotSkipCleanupOrLoseItsFailures() {
        List<String> history = new ArrayList<>();
        TestUser owner = new TestUser(UUID.randomUUID(), "owner_47", "K7e2c9R4");
        TestUser broken = new TestUser(UUID.randomUUID(), "broken_83", "V6m3p8T5");
        TestUser untouched = new TestUser(UUID.randomUUID(), "untouched_29", "Z9n2s7F4");
        try (UserFixture fixture = new UserWithFriendsFixture(
                new FailingUserFixture(owner, history, false, true),
                List.of(new FailingUserFixture(broken, history, true, true),
                        new FailingUserFixture(untouched, history, false, false)))) {
            fixture.create();
            throw new AssertionError("Создание не должно завершаться успешно при ошибке участника");
        } catch (IllegalStateException failure) {
            history.add(failure.getMessage());
            for (Throwable exception : failure.getSuppressed()) {
                history.add(exception.getMessage());
                for (Throwable suppressed : exception.getSuppressed()) {
                    history.add(suppressed.getMessage());
                }
            }
        }
        assertEquals(List.of("create:owner_47", "create:broken_83", "close:broken_83", "close:untouched_29",
                        "close:owner_47", "Не удалось создать пользователя broken_83",
                        "Не удалось очистить пользователя broken_83", "Не удалось очистить пользователя owner_47"),
                history, "Ошибка подготовки не должна прерывать очистку или скрывать её ошибки");
    }
}
