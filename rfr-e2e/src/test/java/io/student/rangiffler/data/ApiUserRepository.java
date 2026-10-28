package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Jdbc;

/**
 * Удаляет профиль тестового пользователя и связанные записи в порядке внешних ключей.
 */
public final class ApiUserRepository {

    public void delete(String username) {
        Jdbc.inTransaction(Databases.API, client -> {
            for (String resource : new String[]{"delete-photo-likes", "delete-likes", "delete-photos",
                    "delete-statistics", "delete-friendships", "delete-user"}) {
                client.sql(Jdbc.sql("sql/api/" + resource + ".sql")).param("username", username).update();
            }
            return null;
        });
    }
}
