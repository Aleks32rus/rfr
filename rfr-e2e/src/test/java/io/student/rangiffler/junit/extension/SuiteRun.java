package io.student.rangiffler.junit.extension;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Владеет хуками одного расширения до их вызова и сохраняет результат после закрытия.
 */
public final class SuiteRun implements SuiteLifecycle {
    private final SuiteExtension extension;
    private final ExtensionContext context;
    private final AtomicReference<State> state;

    public SuiteRun(SuiteExtension extension, ExtensionContext context) {
        this.extension = extension;
        this.context = context;
        this.state = new AtomicReference<>(new State(Phase.PENDING, Optional.empty(), () -> {
        }));
    }

    @Override
    public synchronized void start() {
        if (state.get().phase() == Phase.CLOSED) {
            throw new IllegalStateException("Нельзя начать закрытый запуск " + context.getDisplayName());
        }
        if (state.get().phase() == Phase.STARTING) {
            throw new IllegalStateException("Нельзя повторно войти в подготовку запуска " + context.getDisplayName());
        }
        if (state.get().phase() == Phase.PENDING) {
            state.set(new State(Phase.STARTING, Optional.empty(), () -> {
            }));
            try {
                extension.beforeSuite(context);
                state.set(new State(Phase.STARTED, Optional.empty(), () -> {
                }));
            } catch (RuntimeException | Error failure) {
                state.set(new State(Phase.FAILED, Optional.of(failure), () -> {
                    throw failure;
                }));
            }
        }
        state.get().outcome().run();
    }

    @Override
    public synchronized Optional<Throwable> failure() {
        return state.get().failure();
    }

    @Override
    public synchronized void close() {
        State completion = state.get();
        if (completion.phase() == Phase.CLOSED) {
            return;
        }
        if (completion.phase() == Phase.STARTING) {
            throw new IllegalStateException("Нельзя закрыть запуск во время его подготовки " + context.getDisplayName());
        }
        state.set(new State(Phase.CLOSED, completion.failure(), completion.outcome()));
        if (completion.phase() == Phase.PENDING) {
            return;
        }
        try {
            extension.afterSuite(context);
        } catch (RuntimeException | Error failure) {
            if (completion.failure().isPresent()) {
                if (completion.failure().orElseThrow() != failure) {
                    completion.failure().orElseThrow().addSuppressed(failure);
                }
                completion.outcome().run();
            }
            state.set(new State(Phase.CLOSED, Optional.of(failure), () -> {
                throw failure;
            }));
            throw failure;
        }
        completion.outcome().run();
    }

    /**
     * Не позволяет повторить подготовку или закрытие при конкурентном и повторном обращении.
     */
    private enum Phase {
        PENDING, STARTING, STARTED, FAILED, CLOSED
    }

    /**
     * Сохраняет исходную ошибку и её повторное выбрасывание без изменения типа исключения.
     */
    private record State(Phase phase, Optional<Throwable> failure, Runnable outcome) {
    }
}
