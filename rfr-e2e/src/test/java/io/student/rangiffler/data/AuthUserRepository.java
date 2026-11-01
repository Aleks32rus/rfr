package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Jdbc;

import java.util.UUID;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Создаёт пользователя и его права атомарно по схеме сервиса авторизации.
 */
public final class AuthUserRepository {
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public TestUser create(TestUser user) {
        String password = encoder.encode(user.password());
        return Jdbc.inTransaction(Databases.AUTH, client -> {
            client.sql(Jdbc.sql("sql/auth/insert-user.sql"))
                    .param("id", user.id().toString())
                    .param("username", user.username())
                    .param("password", password)
                    .update();
            client.sql(Jdbc.sql("sql/auth/insert-authorities.sql"))
                    .param("readId", UUID.randomUUID().toString())
                    .param("writeId", UUID.randomUUID().toString())
                    .param("userId", user.id().toString())
                    .update();
            return user;
        });
    }

    public void delete(TestUser user) {
        Jdbc.inTransaction(Databases.AUTH, client -> {
            client.sql(Jdbc.sql("sql/auth/delete-authorities.sql")).param("id", user.id().toString()).update();
            client.sql(Jdbc.sql("sql/auth/delete-user.sql"))
                    .param("id", user.id().toString()).param("username", user.username()).update();
            return null;
        });
    }

    public void delete(String username) {
        Jdbc.inTransaction(Databases.AUTH, client -> {
            client.sql(Jdbc.sql("sql/auth/delete-authorities-by-username.sql"))
                    .param("username", username).update();
            client.sql(Jdbc.sql("sql/auth/delete-user-by-username.sql"))
                    .param("username", username).update();
            return null;
        });
    }
}
