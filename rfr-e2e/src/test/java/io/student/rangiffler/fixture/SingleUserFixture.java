package io.student.rangiffler.fixture;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.data.db.StoredData;
import io.student.rangiffler.utils.RandomDataUtils;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Владеет тестовым пользователем и освобождает его данные в auth и API.
 */
public final class SingleUserFixture implements UserFixture {
    private final TestUser user;
    private final StoredData account;
    private final StoredData profile;
    private final Set<Stage> stages = EnumSet.of(Stage.CLEANUP);

    public SingleUserFixture() {
        this(new TestUser(UUID.randomUUID(), RandomDataUtils.randomUsername(), RandomDataUtils.randomPassword()));
    }

    public SingleUserFixture(TestUser user) {
        this(user, new AuthAccount(user), new Profile(user));
    }

    public SingleUserFixture(TestUser user, StoredData account, StoredData profile) {
        this.user = user;
        this.account = account;
        this.profile = profile;
    }

    @Override
    public TestUser user() {
        return user;
    }

    @Override
    public TestUser create() {
        stages.add(Stage.CLEANUP);
        if (!stages.contains(Stage.AUTH)) {
            account.create();
            stages.add(Stage.AUTH);
        }
        if (!stages.contains(Stage.PROFILE)) {
            profile.create();
            stages.add(Stage.PROFILE);
        }
        return user;
    }

    @Override
    public void close() {
        if (!stages.contains(Stage.CLEANUP)) {
            return;
        }
        new TearDown().with(new Friendships(user)::delete).with(profile::delete).with(account::delete).close();
        stages.clear();
    }

    /**
     * Отмечает завершённые этапы подготовки и необходимость очистки.
     */
    private enum Stage {
        AUTH, PROFILE, CLEANUP
    }
}
