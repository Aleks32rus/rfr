package io.student.rangiffler.data.db;

import io.student.rangiffler.data.TestUser;

import java.util.UUID;

/**
 * Управляет дружбой и направленными приглашениями конкретного пользователя.
 */
public final class Friendships implements Relationships {
    private final TestUser user;

    public Friendships(TestUser user) {
        this.user = user;
    }

    @Override
    public void create(TestUser peer, FriendshipStatus status) {
        if (Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/" + switch (status) {
                    case ACCEPTED -> "create-friendship";
                    case PENDING -> "create-invitation";
                } + ".sql")).param("id", UUID.randomUUID().toString()).param("requesterId", user.id().toString())
                .param("addresseeId", peer.id().toString()).update() != 1) {
            throw new IllegalStateException("Не удалось создать связь пользователей " + user.username()
                    + " и " + peer.username());
        }
    }

    @Override
    public void delete() {
        Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/delete-friendships.sql"))
                .param("username", user.username()).update();
    }

    @Override
    public void delete(FriendshipStatus status) {
        Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/" + switch (status) {
            case ACCEPTED -> "delete-friends";
            case PENDING -> "delete-invitations";
        } + ".sql")).param("username", user.username()).update();
    }

    @Override
    public boolean exist() {
        return Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/count-friendships.sql"))
                .param("username", user.username()).query(Long.class).single() > 0;
    }

    @Override
    public boolean exist(TestUser peer, FriendshipStatus status) {
        return Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/count-friendship.sql"))
                .param("owner", user.id().toString()).param("peer", peer.id().toString()).param("status", status.name())
                .query(Long.class).single() > 0;
    }

    @Override
    public long count(FriendshipStatus status) {
        return Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/count-friendships-by-status.sql"))
                .param("username", user.username()).param("status", status.name()).query(Long.class).single();
    }

    @Override
    public long incomeInvitations() {
        return Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/count-income-invitations.sql"))
                .param("username", user.username()).query(Long.class).single();
    }

    @Override
    public long outcomeInvitations() {
        return Jdbc.client(Databases.API).sql(Jdbc.sql("sql/api/count-outcome-invitations.sql"))
                .param("username", user.username()).query(Long.class).single();
    }
}
