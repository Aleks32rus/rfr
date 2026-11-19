package io.student.rangiffler.fixture;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.data.db.Friendships;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Дополняет пользователя входящими приглашениями и владеет их отправителями.
 */
public final class UserWithIncomeInvitations implements UserFixture {
    private final UserFixture origin;
    private final List<UserFixture> invitations;
    private final Set<UUID> linked;

    public UserWithIncomeInvitations(UserFixture origin, int count) {
        this(origin, Stream.<UserFixture>generate(SingleUserFixture::new).limit(count).toList());
    }

    public UserWithIncomeInvitations(UserFixture origin, List<UserFixture> invitations) {
        this.origin = origin;
        this.invitations = List.copyOf(invitations);
        this.linked = new HashSet<>();
    }

    @Override
    public TestUser user() {
        TestUser user = origin.user();
        return new TestUser(user.id(), user.username(), user.password(),
                Stream.concat(user.incomeInvitations().stream(), invitations.stream()
                        .map(UserFixture::user).filter(invitation -> linked.contains(invitation.id()))).toList(), user.friends(), user.outcomeInvitations());
    }

    @Override
    public TestUser create() {
        TestUser user = origin.create();
        for (UserFixture fixture : invitations) {
            TestUser invitation = fixture.create();
            if (!linked.contains(invitation.id())) {
                new Friendships(invitation).create(user, FriendshipStatus.PENDING);
                linked.add(invitation.id());
            }
        }
        return user();
    }

    @Override
    public void close() {
        new TearDown().withEach(invitations, UserFixture::close).with(origin::close).close();
        linked.clear();
    }
}
