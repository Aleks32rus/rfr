package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserWithOutcomeInvitationsFixture;
import io.student.rangiffler.test.fake.FailingUserFixture;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Подготовка и очистка исходящих приглашений")
final class UserWithOutcomeInvitationsFixtureTest {
    @Test
    @Tag("db")
    @DisplayName("Ошибка профиля приглашённого не оставляет частично подготовленные данные")
    void failedInviteeProfileCannotLeavePreviouslyCreatedParticipants() {
        TestUser broken = new TestUser(UUID.randomUUID(), "broken_" + UUID.randomUUID(), "N8c3R6m9T2p5");
        UserFixture owner = new SingleUserFixture();
        List<UserFixture> invitations = List.of(new SingleUserFixture(),
                new SingleUserFixture(broken, new AuthAccount(broken), new Profile(broken, "missing-country-53")),
                new SingleUserFixture());
        List<String> failures = new ArrayList<>();
        try (UserFixture fixture = new UserWithOutcomeInvitationsFixture(owner, invitations)) {
            try {
                fixture.create();
            } catch (DataIntegrityViolationException failure) {
                failures.add("profile");
            }
        }
        assertEquals(List.of(List.of("profile"), false), List.of(failures,
                        Stream.concat(invitations.stream(), Stream.of(owner)).anyMatch(fixture ->
                                new Profile(fixture.user()).exist() || new AuthAccount(fixture.user()).exist())),
                "Ошибка профиля приглашённого не должна оставлять аккаунты или профили участников");
    }

    @Test
    @Tag("db")
    @DisplayName("Подготовленный пользователь содержит получателей исходящих приглашений")
    void preparedUserCannotLoseOutcomeInvitees() {
        List<UserFixture> invitations = Stream.<UserFixture>generate(SingleUserFixture::new).limit(3).toList();
        try (UserFixture fixture = new UserWithOutcomeInvitationsFixture(new SingleUserFixture(), invitations)) {
            TestUser user = fixture.create();
            assertEquals(invitations.stream().map(UserFixture::user).toList(), user.outcomeInvitations(),
                    "Подготовленный пользователь не должен терять получателей исходящих приглашений");
        }
    }

    @Test
    @Tag("db")
    @DisplayName("Повторная подготовка сохраняет получателей исходящих приглашений")
    void repeatedCreationCannotChangeOutcomeInvitees() {
        try (UserFixture fixture = new UserWithOutcomeInvitationsFixture(new SingleUserFixture(), 4)) {
            TestUser user = fixture.create();
            fixture.create();
            assertEquals(user.outcomeInvitations(), fixture.user().outcomeInvitations(),
                    "Повторная подготовка не должна изменять получателей исходящих приглашений");
        }
    }

    @Test
    @Tag("db")
    @DisplayName("Повторная подготовка не дублирует приглашения и сохраняет их направление")
    void repeatedCreationCannotDuplicateOrReverseOutcomeInvitations() {
        try (UserFixture fixture = new UserWithOutcomeInvitationsFixture(new SingleUserFixture(), 5)) {
            Friendships friendships = new Friendships(fixture.create());
            fixture.create();
            assertEquals(List.of(5L, 0L), List.of(friendships.outcomeInvitations(), friendships.incomeInvitations()),
                    "Приглашения не должны дублироваться или становиться входящими");
        }
    }

    @Test
    @Tag("db")
    @DisplayName("Закрытие фикстуры удаляет всех участников из обеих БД")
    void closingCannotLeaveParticipantsInEitherDatabase() {
        UserFixture owner = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "owner_" + UUID.randomUUID(), "G8p3J6n2D9r4"));
        List<UserFixture> invitations = Stream.<UserFixture>generate(SingleUserFixture::new).limit(3).toList();
        try (UserFixture fixture = new UserWithOutcomeInvitationsFixture(owner, invitations)) {
            fixture.create();
        }
        assertFalse(Stream.concat(invitations.stream(), Stream.of(owner))
                        .anyMatch(fixture -> new Profile(fixture.user()).exist() || new AuthAccount(fixture.user()).exist()),
                "После закрытия участники декоратора не должны оставаться в БД");
    }

    @Test
    @DisplayName("Ошибка подготовки не прерывает очистку и не скрывает её ошибки")
    void failedCreationCannotSkipCleanupOrLoseItsFailures() {
        List<String> history = new ArrayList<>();
        TestUser owner = new TestUser(UUID.randomUUID(), "owner_47", "K7e2c9R4");
        TestUser broken = new TestUser(UUID.randomUUID(), "broken_83", "V6m3p8T5");
        TestUser untouched = new TestUser(UUID.randomUUID(), "untouched_29", "Z9n2s7F4");
        try (UserFixture fixture = new UserWithOutcomeInvitationsFixture(
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
