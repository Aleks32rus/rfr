package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.fixture.ReservedUserFixture;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Проверяет создание и очистку пользователя в auth и API.
 */
@Tag("db")
@DisplayName("Подготовка и очистка одного пользователя")
final class SingleUserFixtureTest {
    @Test
    @DisplayName("Отказ создания профиля не удаляет ранее существовавший профиль")
    void failedProfileCreationCannotDeleteAnotherFixturesProfile() {
        TestUser existing = new TestUser(UUID.randomUUID(), "profile_" + UUID.randomUUID(), "V8n3J6r2K9s4");
        try (UserReservation owner = new ReservedUserFixture(existing)) {
            new Profile(existing).create();
            List<String> failures = new ArrayList<>();
            try (UserFixture duplicate = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                    existing.username(), "G7c2P9w4M6b3"))) {
                duplicate.create();
            } catch (DuplicateKeyException failure) {
                failures.add("duplicate");
            }
            assertThat(List.of(failures, new Profile(existing).exist(),
                    new AuthAccount(existing).exist(), new Authorities(existing).exist()))
                    .as("Отказ подготовки не должен удалять чужой профиль или оставлять созданный аккаунт")
                    .isEqualTo(List.of(List.of("duplicate"), true, false, false));
        }
    }

    @Test
    @DisplayName("Отказ создания с занятым логином не удаляет данные другого владельца")
    void failedCreationCannotDeleteAnotherFixturesUser() {
        try (UserFixture owner = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "owned_" + UUID.randomUUID(), "P8v3R6n2J9s4"))) {
            TestUser user = owner.create();
            List<String> failures = new ArrayList<>();
            try (UserFixture duplicate = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                    user.username(), "T7c2M9k4W6b3"))) {
                duplicate.create();
            } catch (DuplicateKeyException failure) {
                failures.add("duplicate");
            }
            assertThat(List.of(failures, new AuthAccount(user).exist(),
                    new Authorities(user).exist(), new Profile(user).exist()))
                    .as("Отказ создания не должен удалять аккаунт, права или профиль другого владельца")
                    .isEqualTo(List.of(List.of("duplicate"), true, true, true));
        }
    }

    @Test
    @DisplayName("Фикстура создаёт аккаунт и профиль пользователя")
    void creationCannotLeaveUserWithoutProfile() {
        try (UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "fixture_" + UUID.randomUUID(), "M7e4q9K2w8R5"))) {
            TestUser user = fixture.create();
            assertEquals(List.of(true, true, true), List.of(new AuthAccount(user).exist(),
                            new Authorities(user).complete(), new Profile(user).exist()),
                    "Пользователь не должен оставаться без аккаунта, прав или профиля API");
        }
    }

    @Test
    @DisplayName("Повторное закрытие фикстуры безопасно и удаляет аккаунт с профилем")
    void closingCannotLeaveUserAndProfileOrFailWhenRepeated() {
        UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "fixture_" + UUID.randomUUID(), "M7e4q9K2w8R5"));
        TestUser user;
        try (fixture) {
            user = fixture.create();
            fixture.create();
        }
        fixture.close();
        assertEquals(List.of(false, false, false), List.of(new AuthAccount(user).exist(),
                        new Authorities(user).exist(), new Profile(user).exist()),
                "После закрытия аккаунт, права и профиль не должны оставаться в БД");
    }

    @Test
    @DisplayName("Закрытие фикстуры удаляет права пользователя")
    void closingCannotLeaveAuthorities() {
        TestUser user;
        try (UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "rights_" + UUID.randomUUID(), "H8v3J6s9P2c7"))) {
            user = fixture.create();
        }
        assertFalse(new Authorities(user).exist(), "Права пользователя не должны оставаться после закрытия фикстуры");
    }
}
