package io.student.rangiffler.fixture;

import java.util.function.Consumer;

/**
 * Описывает неизменяемую цепочку действий очистки.
 */
public interface Cleanup extends AutoCloseable {
    TearDown with(Runnable action);

    <T> TearDown withEach(Iterable<? extends T> values, Consumer<? super T> action);

    @Override
    void close();
}
