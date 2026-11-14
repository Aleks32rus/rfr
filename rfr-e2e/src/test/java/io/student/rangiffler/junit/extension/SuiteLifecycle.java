package io.student.rangiffler.junit.extension;

import java.util.Optional;

/**
 * Определяет однократную подготовку и закрытие запуска с сохранением причины ошибки.
 */
public interface SuiteLifecycle extends AutoCloseable {
    void start();

    Optional<Throwable> failure();

    @Override
    void close();
}
