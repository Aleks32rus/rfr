package io.student.rangiffler.fixture;

import io.student.rangiffler.data.TestUser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Владеет временно выданным пользователем общего пула и возвращает его без изменения БД.
 */
public final class StaticUserFixture implements UserFixture {
    private static final BlockingQueue<TestUser> USERS = new ArrayBlockingQueue<>(3, true, List.of(
            new TestUser(UUID.randomUUID(), "pool_alex_17", "Queue8n3K6m2"),
            new TestUser(UUID.randomUUID(), "pool_maria_43", "Queue5r9T2p7"),
            new TestUser(UUID.randomUUID(), "pool_ivan_29", "Queue4s8V3w6")));
    private final Duration timeout;
    private final List<TestUser> lease;

    public StaticUserFixture() {
        this(Duration.ofSeconds(3));
    }

    public StaticUserFixture(Duration timeout) {
        this.timeout = timeout;
        this.lease = new ArrayList<>(1);
    }

    @Override
    public synchronized TestUser user() {
        return lease.stream().findFirst().orElseThrow(() ->
                new IllegalStateException("Пользователь из общего пула ещё не получен"));
    }

    @Override
    public synchronized TestUser create() {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Таймаут получения пользователя из пула должен быть положительным");
        }
        if (lease.isEmpty()) {
            try {
                lease.add(Optional.ofNullable(USERS.poll(timeout.toNanos(), TimeUnit.NANOSECONDS))
                        .orElseThrow(() -> new IllegalStateException("Не удалось получить пользователя из пула за "
                                + timeout + ": все три пользователя заняты")));
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ожидание свободного пользователя из пула прервано", failure);
            }
        }
        return user();
    }

    @Override
    public synchronized void close() {
        if (!lease.isEmpty()) {
            if (!USERS.offer(lease.getFirst())) {
                throw new IllegalStateException("Не удалось вернуть пользователя в заполненный пул");
            }
            lease.clear();
        }
    }
}
