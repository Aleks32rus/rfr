package io.student.rangiffler.test.fake;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.fixture.UserFixture;

import java.util.List;

/**
 * Фиксирует этапы жизненного цикла и воспроизводит ошибки подготовки и очистки.
 */
public final class FailingUserFixture implements UserFixture {
    private final TestUser user;
    private final List<String> history;
    private final boolean creation;
    private final boolean cleanup;

    public FailingUserFixture(TestUser user, List<String> history, boolean creation, boolean cleanup) {
        this.user = user;
        this.history = history;
        this.creation = creation;
        this.cleanup = cleanup;
    }

    @Override
    public TestUser user() {
        return user;
    }

    @Override
    public TestUser create() {
        history.add("create:" + user.username());
        if (creation) {
            throw new IllegalStateException("Не удалось создать пользователя " + user.username());
        }
        return user;
    }

    @Override
    public void close() {
        history.add("close:" + user.username());
        if (cleanup) {
            throw new IllegalStateException("Не удалось очистить пользователя " + user.username());
        }
    }
}
