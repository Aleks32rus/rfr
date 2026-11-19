package io.student.rangiffler.fixture;

import io.student.rangiffler.data.TestUser;

/**
 * Управляет подготовкой и освобождением данных тестового пользователя.
 */
public interface UserFixture extends AutoCloseable {
    TestUser user();

    TestUser create();

    @Override
    void close();
}
