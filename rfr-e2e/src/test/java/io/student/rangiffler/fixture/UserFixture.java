package io.student.rangiffler.fixture;

import io.student.rangiffler.data.ApiUserRepository;
import io.student.rangiffler.data.AuthUserRepository;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.utils.RandomDataUtils;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/**
 * Владеет тестовым пользователем и освобождает его данные в auth и API.
 */
public final class UserFixture implements AutoCloseable {
    private final AuthUserRepository auth = new AuthUserRepository();
    private final ApiUserRepository api = new ApiUserRepository();
    private final TestUser user = new TestUser(UUID.randomUUID(),
            RandomDataUtils.randomUsername(), RandomDataUtils.randomPassword());
    private boolean created;
    private boolean cleanupRequired = true;

    public TestUser user() {
        return user;
    }

    public TestUser create() {
        if (!created) {
            auth.create(user);
            created = true;
            cleanupRequired = true;
        }
        return user;
    }

    @Override
    public void close() {
        if (!cleanupRequired) {
            return;
        }
        List<Runnable> actions = List.of(
                () -> api.delete(user.username()),
                () -> auth.delete(user.username()));
        List<RuntimeException> exceptions = new ArrayList<>();
        for (Runnable action : actions) {
            try {
                action.run();
            } catch (RuntimeException exception) {
                exceptions.add(exception);
            }
        }
        if (!exceptions.isEmpty()) {
            RuntimeException failure = exceptions.getFirst();
            exceptions.stream().skip(1).forEach(failure::addSuppressed);
            throw failure;
        }
        created = false;
        cleanupRequired = false;
    }
}
