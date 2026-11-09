package io.student.rangiffler.test.db;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет независимое управление правами существующего аккаунта.
 */
@Tag("db")
@DisplayName("Обязательные права пользователя")
final class AuthoritiesTest {
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
            assertTrue(authorities.exist(), "Повторно созданные права не должны отсутствовать");
        }
    }
}
