package io.student.rangiffler.fixture;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.utils.RandomDataUtils;

import java.util.UUID;

/**
 * Освобождает данные выделенного тесту логина после внешней подготовки или UI-регистрации.
 */
public final class ReservedUserFixture implements UserReservation {
    private final TestUser user;

    public ReservedUserFixture() {
        this(new TestUser(UUID.randomUUID(), RandomDataUtils.randomUsername(), RandomDataUtils.randomPassword()));
    }

    public ReservedUserFixture(TestUser user) {
        this.user = user;
    }

    @Override
    public TestUser user() {
        return user;
    }

    @Override
    public void close() {
        new TearDown().with(new Profile(user)::delete).with(new AuthAccount(user)::delete).close();
    }
}
