package io.student.rangiffler.data.db;

import io.student.rangiffler.data.TestUser;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Атомарно создаёт и удаляет аккаунт вместе с его обязательными правами.
 */
public final class AuthAccount implements StoredData {
    private final TestUser user;
    private final PasswordEncoder encoder;

    public AuthAccount(TestUser user) {
        this(user, PasswordEncoderFactories.createDelegatingPasswordEncoder());
    }

    public AuthAccount(TestUser user, PasswordEncoder encoder) {
        this.user = user;
        this.encoder = encoder;
    }

    @Override
    public void create() {
        Jdbc.inTransaction(Databases.AUTH, client -> {
            if (client.sql(Jdbc.sql("sql/auth/insert-user.sql"))
                    .param("id", user.id().toString()).param("username", user.username())
                    .param("password", encoder.encode(user.password())).update() != 1) {
                throw new IllegalStateException("Не удалось создать аккаунт " + user.username());
            }
            new Authorities(user).create();
            return Boolean.TRUE;
        });
    }

    @Override
    public void delete() {
        Jdbc.inTransaction(Databases.AUTH, client -> {
            new Authorities(user).delete();
            client.sql(Jdbc.sql("sql/auth/delete-user-by-username.sql")).param("username", user.username()).update();
            return Boolean.TRUE;
        });
    }

    @Override
    public boolean exist() {
        return Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/count-user-by-username.sql"))
                .param("username", user.username()).query(Long.class).single() > 0;
    }
}
