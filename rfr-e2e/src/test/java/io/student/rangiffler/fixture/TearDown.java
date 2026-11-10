package io.student.rangiffler.fixture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Выполняет зарегистрированные действия очистки, сохраняя ошибки всех участников.
 */
public final class TearDown implements Cleanup {
    private final List<Runnable> actions;

    public TearDown() {
        this(List.of());
    }

    public TearDown(List<Runnable> actions) {
        this.actions = List.copyOf(actions);
    }

    @Override
    public TearDown with(Runnable action) {
        return new TearDown(Stream.concat(actions.stream(), Stream.of(action)).toList());
    }

    @Override
    public <T> TearDown withEach(Iterable<? extends T> values, Consumer<? super T> action) {
        Objects.requireNonNull(action, "Не задано действие очистки элементов");
        return new TearDown(Stream.concat(actions.stream(),
                StreamSupport.stream(values.spliterator(), false)
                        .<Runnable>map(value -> () -> action.accept(value))).toList());
    }

    @Override
    public void close() {
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
            exceptions.stream().skip(1).filter(exception -> exception != failure).forEach(failure::addSuppressed);
            throw failure;
        }
    }
}
