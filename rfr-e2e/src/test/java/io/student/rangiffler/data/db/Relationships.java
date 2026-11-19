package io.student.rangiffler.data.db;

import io.student.rangiffler.data.TestUser;

/**
 * Описывает операции со связями конкретного пользователя.
 */
public interface Relationships {
    void create(TestUser peer, FriendshipStatus status);

    void delete();

    void delete(FriendshipStatus status);

    boolean exist();

    boolean exist(TestUser peer, FriendshipStatus status);

    long count(FriendshipStatus status);

    long incomeInvitations();

    long outcomeInvitations();
}
