package io.student.rangiffler.data.db;

import io.student.rangiffler.data.TestUser;

import java.util.UUID;

/**
 * Управляет обязательными правами read и write конкретного аккаунта.
 */
public final class Authorities implements StoredData {
    private final TestUser user;

    public Authorities(TestUser user) {
        this.user = user;
    }

    @Override
    public void create() {
        if (Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/insert-authorities.sql"))
                .param("readId", UUID.randomUUID().toString()).param("writeId", UUID.randomUUID().toString())
                .param("userId", user.id().toString()).update() != 2) {
            throw new IllegalStateException("Не удалось создать права аккаунта " + user.username());
        }
    }

    @Override
    public void delete() {
        Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/delete-authorities-by-username.sql"))
                .param("username", user.username()).update();
    }

    @Override
    public boolean exist() {
        return Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/count-authorities-by-username.sql"))
                .param("username", user.username()).query(Long.class).single() == 2;
    }
}
