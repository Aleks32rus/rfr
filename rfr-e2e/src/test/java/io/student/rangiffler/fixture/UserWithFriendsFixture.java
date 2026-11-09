package io.student.rangiffler.fixture;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.FriendshipStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Дополняет пользователя друзьями и владеет их фикстурами.
 */
public final class UserWithFriendsFixture implements UserFixture {
    private final UserFixture origin;
    private final List<UserFixture> friends;
    private final Set<UUID> linked;

    public UserWithFriendsFixture(UserFixture origin, int count) {
        this(origin, Stream.<UserFixture>generate(SingleUserFixture::new).limit(count).toList());
    }

    public UserWithFriendsFixture(UserFixture origin, List<UserFixture> friends) {
        this.origin = origin;
        this.friends = List.copyOf(friends);
        this.linked = new HashSet<>();
    }

    @Override
    public TestUser user() {
        TestUser user = origin.user();
        return new TestUser(user.id(), user.username(), user.password(), user.incomeInvitations(),
                Stream.concat(user.friends().stream(), friends.stream()
                        .map(UserFixture::user).filter(friend -> linked.contains(friend.id()))).toList());
    }

    @Override
    public TestUser create() {
        TestUser user = origin.create();
        for (UserFixture fixture : friends) {
            TestUser friend = fixture.create();
            if (!linked.contains(friend.id())) {
                new Friendships(user).create(friend, FriendshipStatus.ACCEPTED);
                linked.add(friend.id());
            }
        }
        return user();
    }

    @Override
    public void close() {
        new TearDown().withEach(friends, UserFixture::close).with(origin::close).close();
        linked.clear();
    }
}
