package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.fixture.ReservedUserFixture;
import io.student.rangiffler.fixture.UserReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет освобождение данных выделенного логина с UUID внешней регистрации.
 */
@Tag("db")
@DisplayName("Очистка внешней регистрации пользователя")
final class ReservedUserFixtureTest {
    @Test
    @DisplayName("Внешний UUID не мешает очистке зарегистрированного пользователя")
    void externalRegistrationCannotEscapeCleanupWithDifferentId() {
        TestUser reserved = new TestUser(UUID.randomUUID(), "registered_" + UUID.randomUUID(), "N8v3W6r2P9s4");
        TestUser actual = new TestUser(UUID.randomUUID(), reserved.username(), "J7c2M9k4T6b3");
        List<Boolean> history = new ArrayList<>();
        try (UserReservation fixture = new ReservedUserFixture(reserved)) {
            new AuthAccount(actual).create();
            new Profile(actual).create();
            history.add(new Authorities(actual).complete());
        }
        history.add(new AuthAccount(actual).exist());
        history.add(new Authorities(actual).exist());
        history.add(new Profile(actual).exist());
        assertThat(history)
                .as("Внешний UUID не должен мешать очистке аккаунта, прав и профиля выделенного логина")
                .containsExactly(true, false, false, false);
    }
}
