package io.student.rangiffler.fixture;

import io.student.rangiffler.data.TestUser;

/**
 * Определяет владение выделенным логином для создания данных вне фикстуры.
 */
public interface UserReservation extends AutoCloseable {
    TestUser user();

    @Override
    void close();
}
