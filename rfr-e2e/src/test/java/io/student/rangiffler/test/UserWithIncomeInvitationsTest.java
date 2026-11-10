package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserWithIncomeInvitations;
import io.student.rangiffler.test.fake.FailingUserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Проверяет подготовку связей и очистку всех участников декоратора.
 */
@DisplayName("Подготовка и очистка входящих приглашений")
final class UserWithIncomeInvitationsTest {
    @Test
    @DisplayName("Добавление входящих приглашений сохраняет исходящие приглашения вложенной фикстуры")
    void incomeInvitationsCannotDiscardPreparedOutcomeInvitations() {
        TestUser invitation = new TestUser(UUID.randomUUID(), "invitee_" + UUID.randomUUID(), "N8r3W6c2J9p4");
        TestUser user = new TestUser(UUID.randomUUID(), "owner_" + UUID.randomUUID(), "B7s2K9v4M6n3")
                .withOutcomeInvitations(List.of(invitation));
        try (UserFixture fixture = new UserWithIncomeInvitations(
                new FailingUserFixture(user, new ArrayList<>(), false, false), List.of())) {
            assertThat(fixture.create().outcomeInvitations())
                    .as("Декоратор входящих приглашений не должен терять исходящие приглашения вложенной фикстуры")
                    .containsExactly(invitation);
        }
    }

    @Test
    @Tag("db")
    @DisplayName("Подготовленный пользователь содержит отправителей входящих приглашений")
    void preparedUserCannotLoseIncomeInvitationSenders() {
        List<UserFixture> invitations = Stream.<UserFixture>generate(SingleUserFixture::new).limit(3).toList();
        try (UserFixture fixture = new UserWithIncomeInvitations(new SingleUserFixture(), invitations)) {
            TestUser user = fixture.create();
            assertEquals(invitations.stream().map(UserFixture::user).toList(), user.incomeInvitations(),
                    "Подготовленный пользователь не должен терять отправителей входящих приглашений");
        }
    }

    @Test
    @Tag("db")
    @DisplayName("Повторная подготовка сохраняет отправителей входящих приглашений")
    void repeatedCreationCannotChangeIncomeInvitationSenders() {
        try (UserFixture fixture = new UserWithIncomeInvitations(new SingleUserFixture(), 4)) {
            TestUser user = fixture.create();
            fixture.create();
            assertEquals(user.incomeInvitations(), fixture.user().incomeInvitations(),
                    "Повторная подготовка не должна изменять отправителей входящих приглашений");
        }
    }

    @Test
    @Tag("db")
    @DisplayName("Повторная подготовка не дублирует приглашения и сохраняет их направление")
    void repeatedCreationCannotDuplicateOrReverseIncomeInvitations() {
        try (UserFixture fixture = new UserWithIncomeInvitations(new SingleUserFixture(), 5)) {
            Friendships friendships = new Friendships(fixture.create());
            fixture.create();
            assertEquals(List.of(5L, 0L), List.of(friendships.incomeInvitations(), friendships.outcomeInvitations()),
                    "Приглашения не должны дублироваться или становиться исходящими");
        }
    }

    @Test
    @Tag("db")
    @DisplayName("Закрытие фикстуры удаляет всех участников из обеих БД")
    void closingCannotLeaveParticipantsInEitherDatabase() {
        UserFixture owner = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "owner_" + UUID.randomUUID(), "G8p3J6n2D9r4"));
        List<UserFixture> invitations = Stream.<UserFixture>generate(SingleUserFixture::new).limit(3).toList();
        try (UserFixture fixture = new UserWithIncomeInvitations(owner, invitations)) {
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
        try (UserFixture fixture = new UserWithIncomeInvitations(
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
