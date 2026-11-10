package io.student.rangiffler.test.db;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.data.db.Authority;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет независимое управление правами существующего аккаунта.
 */
@Tag("db")
@DisplayName("Обязательные права пользователя")
final class AuthoritiesTest {
    @ParameterizedTest
    @ValueSource(strings = {"read", "write"})
    @DisplayName("Единственное право не считается полным обязательным набором")
    void singleAuthorityCannotBeReportedAsComplete(String name) {
        try (UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "incomplete_" + UUID.randomUUID(), "Q8n3V6r2M9s4"))) {
            TestUser user = fixture.create();
            new Authorities(user).delete();
            new Authority(user, name).create();
            assertThat(new Authorities(user).complete())
                    .as("Одно право не должно считаться полным набором read и write")
                    .isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"read", "write"})
    @DisplayName("Единственное оставшееся право не считается отсутствующим")
    void remainingSingleAuthorityCannotBeReportedAsAbsent(String name) {
        try (UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "partial_" + UUID.randomUUID(), "W8c3N6p2R9s4"))) {
            TestUser user = fixture.create();
            new Authorities(user).delete();
            new Authority(user, name).create();
            assertThat(new Authorities(user).exist())
                    .as("Проверка наличия прав не должна скрывать единственную оставшуюся строку")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Удалённые права пользователя отсутствуют в БД")
    void deletedAuthoritiesCannotBeReportedAsExisting() {
        try (UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "rights_" + UUID.randomUUID(), "R4b8X2s7N9m3"))) {
            Authorities authorities = new Authorities(fixture.create());
            authorities.delete();
            assertEquals(List.of(false, true), List.of(authorities.exist(), new AuthAccount(fixture.user()).exist()),
                    "Удалённые права не должны существовать или удалять аккаунт");
        }
    }

    @Test
    @DisplayName("Повторное создание восстанавливает права пользователя")
    void recreatedAuthoritiesCannotRemainAbsent() {
        try (UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "rights_" + UUID.randomUUID(), "Y7p3L8c2H9v4"))) {
            Authorities authorities = new Authorities(fixture.create());
            authorities.delete();
            authorities.create();
            assertTrue(authorities.complete(), "Повторно созданные права не должны отсутствовать");
        }
    }
}
