package io.student.rangiffler.test;

import io.student.rangiffler.data.AuthUserRepository;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.utils.RandomDataUtils;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Проверяет доступ к обеим БД и откат записи пользователя при ошибке транзакции.
 */
@Tag("db")
@DisplayName("Инфраструктура JDBC")
public class JdbcTest {

    @ParameterizedTest
    @EnumSource(Databases.class)
    @DisplayName("JDBC-клиент подключается к выбранной БД")
    void databaseIsAccessible(Databases database) {
        assertEquals(1, Jdbc.client(database).sql(Jdbc.sql("sql/ping.sql")).query(Integer.class).single(),
                "Не удалось выполнить запрос в выбранной БД");
    }

    @Test
    @DisplayName("Ошибка внешней транзакции откатывает создание пользователя")
    void failedTransactionDoesNotPersistUser() {
        TestUser user = new TestUser(UUID.randomUUID(), RandomDataUtils.randomUsername(), RandomDataUtils.randomPassword());
        assertThrows(IllegalStateException.class, () -> Jdbc.inTransaction(Databases.AUTH, client -> {
            new AuthUserRepository().create(user);
            throw new IllegalStateException("Преднамеренная ошибка для проверки отката");
        }));
        assertEquals(0L, Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/count-user.sql"))
                        .param("id", user.id().toString()).query(Long.class).single(),
                "Пользователь не должен сохраняться после отката транзакции");
    }
}
