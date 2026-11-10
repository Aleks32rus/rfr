package io.student.rangiffler.data.db;

import io.student.rangiffler.data.TestUser;

/**
 * Управляет профилем пользователя и удаляет зависимые данные в порядке внешних ключей.
 */
public final class Profile implements StoredData {
    private final TestUser user;
    private final String country;

    public Profile(TestUser user) {
        this(user, "ru");
    }

    public Profile(TestUser user, String country) {
        this.user = user;
        this.country = country;
    }

    @Override
    public void create() {
        if (Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/create-profile.sql"))
                .param("id", user.id().toString()).param("username", user.username())
                .param("countryCode", country).update() != 1) {
            throw new IllegalStateException("Не удалось создать профиль пользователя " + user.username());
        }
    }

    @Override
    public void delete() {
        Jdbc.inTransaction(Databases.API, client -> {
            for (String resource : new String[]{"delete-photo-likes", "delete-likes", "delete-photos", "delete-statistics"}) {
                client.sql(Jdbc.sql("sql/api/" + resource + ".sql")).param("username", user.username()).update();
            }
            new Friendships(user).delete();
            client.sql(Jdbc.sql("sql/api/delete-user.sql")).param("username", user.username()).update();
            return Boolean.TRUE;
        });
    }

    @Override
    public boolean exist() {
        return Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/count-user.sql"))
                .param("username", user.username()).query(Long.class).single() > 0;
    }
}
