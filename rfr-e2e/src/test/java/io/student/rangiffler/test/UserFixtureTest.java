package io.student.rangiffler.test;

import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.data.TestUser;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Проверяет очистку данных fixture при обычном завершении и исключении.
 */
@Tag("db")
@DisplayName("Жизненный цикл пользователя")
public class UserFixtureTest {

    @Test
    @DisplayName("Закрытие удаляет пользователя, права и профиль и допускает повторный вызов")
    void closeRemovesUserAndProfile() {
        TestUser user;
        UserFixture fixture = new UserFixture();
        try (fixture) {
            user = fixture.create();
            assertEquals(user, fixture.create(), "Повторное создание не должно создавать другого пользователя");
            assertEquals(1, Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/insert-test-profile.sql"))
                            .param("id", UUID.randomUUID().toString()).param("username", user.username()).update(),
                    "Профиль для проверки очистки не был создан");
        }
        fixture.close();
        assertAll(
                () -> assertEquals(0L, Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/count-user.sql"))
                        .param("id", user.id().toString()).query(Long.class).single(), "Пользователь auth не удалён"),
                () -> assertEquals(0L, Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/count-authorities.sql"))
                        .param("id", user.id().toString()).query(Long.class).single(), "Права пользователя не удалены"),
                () -> assertEquals(0L, Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/count-user.sql"))
                        .param("username", user.username()).query(Long.class).single(), "Профиль API не удалён"));
    }

    @Test
    @DisplayName("Исключение в сценарии не мешает очистке пользователя")
    void failedScenarioDoesNotLeaveUser() {
        UserFixture fixture = new UserFixture();
        TestUser user = fixture.create();
        assertThrows(IllegalStateException.class, () -> {
            try (fixture) {
                throw new IllegalStateException("Преднамеренная ошибка сценария");
            }
        });
        assertEquals(0L, Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/count-user.sql"))
                .param("id", user.id().toString()).query(Long.class).single(), "После ошибки пользователь не удалён");
    }
}
