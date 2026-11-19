package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяет доступ к обеим БД и откат записи аккаунта при ошибке транзакции.
 */
@Tag("db")
@DisplayName("Инфраструктура JDBC")
final class JdbcTest {
    @ParameterizedTest
    @EnumSource(Databases.class)
    @DisplayName("JDBC-клиент подключается к выбранной БД")
    void databaseCannotBeInaccessible(Databases database) {
        assertEquals(1, Jdbc.client(database).sql(Jdbc.sql("sql/ping.sql")).query(Integer.class).single(),
                "Запрос к выбранной БД не должен оставаться невыполненным");
    }

    @Test
    @DisplayName("Ошибка внешней транзакции откатывает создание аккаунта")
    void failedTransactionCannotPersistAccount() {
        TestUser user = new TestUser(UUID.randomUUID(), RandomDataUtils.randomUsername(), RandomDataUtils.randomPassword());
        try {
            Jdbc.inTransaction(Databases.AUTH, client -> {
                new AuthAccount(user).create();
                throw new IllegalStateException("Преднамеренная ошибка для проверки отката");
            });
            throw new AssertionError("Транзакция не должна завершаться успешно при ошибке");
        } catch (IllegalStateException failure) {
            assertEquals(List.of("Преднамеренная ошибка для проверки отката", "false"),
                    List.of(failure.getMessage(), Boolean.toString(new AuthAccount(user).exist())),
                    "Ошибка транзакции не должна скрываться или сохранять аккаунт");
        }
    }
}
