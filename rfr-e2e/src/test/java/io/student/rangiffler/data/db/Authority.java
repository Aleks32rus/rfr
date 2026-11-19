package io.student.rangiffler.data.db;

import io.student.rangiffler.data.TestUser;

import java.util.UUID;

/**
 * Управляет отдельным правом аккаунта для проверки неполного набора прав.
 */
public final class Authority implements StoredData {
    private final TestUser user;
    private final String name;

    public Authority(TestUser user, String name) {
        this.user = user;
        this.name = name;
    }

    @Override
    public void create() {
        if (Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/insert-authority.sql"))
                .param("id", UUID.randomUUID().toString()).param("userId", user.id().toString())
                .param("authority", name).update() != 1) {
            throw new IllegalStateException("Не удалось создать право " + name + " аккаунта " + user.username());
        }
    }

    @Override
    public void delete() {
        Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/delete-authority.sql"))
                .param("userId", user.id().toString()).param("authority", name).update();
    }

    @Override
    public boolean exist() {
        return Jdbc.client(Databases.AUTH).sql(Jdbc.sql("sql/auth/count-authority.sql"))
                .param("userId", user.id().toString()).param("authority", name).query(Long.class).single() > 0;
    }
}
