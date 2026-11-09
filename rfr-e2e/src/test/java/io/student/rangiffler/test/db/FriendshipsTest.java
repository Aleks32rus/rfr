package io.student.rangiffler.test.db;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет симметрию дружбы, направление заявок и выборочное удаление связей.
 */
@Tag("db")
final class FriendshipsTest {
    @Test
    void friendshipCannotDependOnRequesterDirection() {
        try (UserFixture owner = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "owner_" + UUID.randomUUID(), "B8f3N6r2V9p4"));
             UserFixture peer = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                     "peer_" + UUID.randomUUID(), "J4s9T2m7C8v3"))) {
            new Friendships(owner.create()).create(peer.create(), FriendshipStatus.ACCEPTED);
            assertTrue(new Friendships(peer.user()).exist(owner.user(), FriendshipStatus.ACCEPTED),
                    "Дружба не должна зависеть от того, кто отправил исходное приглашение");
        }
    }

    @Test
    void pendingInvitationCannotReverseDirection() {
        try (UserFixture owner = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "owner_" + UUID.randomUUID(), "F2c7W9n4R8p3"));
             UserFixture peer = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                     "peer_" + UUID.randomUUID(), "T6m3K8v2P9b4"))) {
            new Friendships(owner.create()).create(peer.create(), FriendshipStatus.PENDING);
            assertEquals(List.of(true, false), List.of(
                            new Friendships(owner.user()).exist(peer.user(), FriendshipStatus.PENDING),
                            new Friendships(peer.user()).exist(owner.user(), FriendshipStatus.PENDING)),
                    "Входящее приглашение не должно становиться исходящим при проверке второго участника");
        }
    }

    @Test
    void deletingFriendsCannotDeletePendingInvitations() {
        try (UserFixture owner = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "owner_" + UUID.randomUUID(), "G8r3H7m2D9v4"));
             UserFixture friend = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                     "friend_" + UUID.randomUUID(), "V3n9C4p7J2s8"));
             UserFixture sender = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                     "sender_" + UUID.randomUUID(), "N4b7W2r9F3k8"))) {
            Friendships friendships = new Friendships(owner.create());
            friendships.create(friend.create(), FriendshipStatus.ACCEPTED);
            new Friendships(sender.create()).create(owner.user(), FriendshipStatus.PENDING);
            friendships.delete(FriendshipStatus.ACCEPTED);
            assertEquals(List.of(false, true), List.of(friendships.exist(friend.user(), FriendshipStatus.ACCEPTED),
                            new Friendships(sender.user()).exist(owner.user(), FriendshipStatus.PENDING)),
                    "Удаление друзей не должно затрагивать ожидающие приглашения");
        }
    }
}
