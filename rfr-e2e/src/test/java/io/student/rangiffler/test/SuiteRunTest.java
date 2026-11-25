package io.student.rangiffler.test;

import io.student.rangiffler.junit.extension.SuiteExtension;
import io.student.rangiffler.junit.extension.SuiteLifecycle;
import io.student.rangiffler.junit.extension.SuiteRun;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Проверяет сохранение результата подготовки SuiteRun после однократного закрытия.
 */
@ExtendWith(SuiteRunTest.CurrentContext.class)
@DisplayName("Состояние SuiteRun после подготовки и закрытия")
final class SuiteRunTest {
    @Test
    @DisplayName("Ошибка подготовки остаётся доступной после закрытия")
    void closureCannotErasePreparationFailure(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("Подготовка ресурса 73 завершилась ошибкой");
        SuiteLifecycle run = new SuiteRun(new RecordingSuite(events, () -> {
            throw failure;
        }, () -> {
        }), context);
        Optional<Throwable> preparation = Optional.ofNullable(catchThrowable(run::start));
        Optional<Throwable> closure = Optional.ofNullable(catchThrowable(run::close));
        assertThat(List.of(events, preparation, closure, run.failure()))
                .as("Закрытие не должно стирать или подменять исходную ошибку подготовки")
                .isEqualTo(List.of(List.of("before", "after"), Optional.of(failure), Optional.of(failure), Optional.of(failure)));
    }

    @Test
    @DisplayName("Ошибка закрытия добавляется к исходной ошибке подготовки как suppressed")
    void closureFailureCannotReplacePreparationFailure(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        RuntimeException preparation = new IllegalStateException("Не удалось подготовить ресурс 41");
        RuntimeException closure = new IllegalArgumentException("Не удалось освободить ресурс 41");
        SuiteLifecycle run = new SuiteRun(new RecordingSuite(events, () -> {
            throw preparation;
        }, () -> {
            throw closure;
        }), context);
        Optional<Throwable> first = Optional.ofNullable(catchThrowable(run::start));
        Optional<Throwable> last = Optional.ofNullable(catchThrowable(run::close));
        assertThat(List.of(events, first, last, run.failure(), Arrays.asList(preparation.getSuppressed())))
                .as("Ошибка закрытия не должна подменять исходную ошибку подготовки или теряться")
                .isEqualTo(List.of(List.of("before", "after"), Optional.of(preparation), Optional.of(preparation),
                        Optional.of(preparation), List.of(closure)));
    }

    @Test
    @DisplayName("Ошибка закрытия сохраняется и после повторного close")
    void repeatedClosureCannotEraseCleanupFailure(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("Закрытие ресурса 97 завершилось ошибкой");
        SuiteLifecycle run = new SuiteRun(new RecordingSuite(events, () -> {
        }, () -> {
            throw failure;
        }), context);
        run.start();
        Optional<Throwable> closure = Optional.ofNullable(catchThrowable(run::close));
        run.close();
        assertThat(List.of(events, closure, run.failure()))
                .as("Повторное закрытие не должно повторять очистку или стирать её ошибку")
                .isEqualTo(List.of(List.of("before", "after"), Optional.of(failure), Optional.of(failure)));
    }

    @Test
    @DisplayName("Один объект исключения в обоих хуках не добавляется сам к себе")
    void sharedFailureCannotSuppressItself(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("Общая ошибка ресурса 59");
        SuiteLifecycle run = new SuiteRun(new RecordingSuite(events, () -> {
            throw failure;
        }, () -> {
            throw failure;
        }), context);
        Optional<Throwable> preparation = Optional.ofNullable(catchThrowable(run::start));
        Optional<Throwable> closure = Optional.ofNullable(catchThrowable(run::close));
        assertThat(List.of(events, preparation, closure, run.failure(), Arrays.asList(failure.getSuppressed())))
                .as("Одинаковая ошибка обоих хуков не должна приводить к самоподавлению или подмене исключения")
                .isEqualTo(List.of(List.of("before", "after"), Optional.of(failure), Optional.of(failure),
                        Optional.of(failure), List.of()));
    }

    @Test
    @DisplayName("Повторная подготовка возвращает сохранённую ошибку без нового вызова beforeSuite")
    void repeatedPreparationCannotRetryFailedHook(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        RuntimeException failure = new IllegalArgumentException("Подготовка ресурса 83 отклонена");
        SuiteLifecycle run = new SuiteRun(new RecordingSuite(events, () -> {
            throw failure;
        }, () -> {
        }), context);
        Optional<Throwable> first = Optional.ofNullable(catchThrowable(run::start));
        Optional<Throwable> second = Optional.ofNullable(catchThrowable(run::start));
        Optional<Throwable> closure = Optional.ofNullable(catchThrowable(run::close));
        assertThat(List.of(events, first, second, closure, run.failure()))
                .as("Повторное обращение не должно повторять неуспешную подготовку или терять её причину")
                .isEqualTo(List.of(List.of("before", "after"), Optional.of(failure), Optional.of(failure),
                        Optional.of(failure), Optional.of(failure)));
    }

    @Test
    @DisplayName("Error подготовки сохраняет исходный тип и не отменяет закрытие")
    void preparationErrorCannotChangeTypeOrSkipClosure(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        Error failure = new AssertionError("Ошибка подготовки ресурса 67");
        SuiteLifecycle run = new SuiteRun(new RecordingSuite(events, () -> {
            throw failure;
        }, () -> {
        }), context);
        Optional<Throwable> preparation = Optional.ofNullable(catchThrowable(run::start));
        Optional<Throwable> closure = Optional.ofNullable(catchThrowable(run::close));
        assertThat(List.of(events, preparation, closure, run.failure()))
                .as("Error подготовки не должен менять исходный тип или отменять закрытие")
                .isEqualTo(List.of(List.of("before", "after"), Optional.of(failure), Optional.of(failure), Optional.of(failure)));
    }

    @Test
    @DisplayName("Повторные start и close не повторяют успешные хуки")
    void repeatedCallsCannotRepeatSuccessfulHooks(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        SuiteLifecycle run = new SuiteRun(new RecordingSuite(events, () -> {
        }, () -> {
        }), context);
        run.start();
        run.start();
        run.close();
        run.close();
        assertThat(events)
                .as("Повторные вызовы не должны повторять успешную подготовку или закрытие")
                .containsExactly("before", "after");
    }

    /**
     * Даёт тестируемому запуску настоящий контекст Jupiter без имитации его хранилища.
     */
    public static final class CurrentContext implements ParameterResolver {
        @Override
        public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
            return parameter.getParameter().getType().equals(ExtensionContext.class);
        }

        @Override
        public ExtensionContext resolveParameter(ParameterContext parameter, ExtensionContext context) {
            return context;
        }
    }

    /**
     * Задаёт независимые результаты хуков и фиксирует их фактические вызовы.
     */
    private static final class RecordingSuite implements SuiteExtension {
        private final List<String> events;
        private final Runnable preparation;
        private final Runnable closure;

        private RecordingSuite(List<String> events, Runnable preparation, Runnable closure) {
            this.events = events;
            this.preparation = preparation;
            this.closure = closure;
        }

        @Override
        public void beforeSuite(ExtensionContext context) {
            events.add("before");
            preparation.run();
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            events.add("after");
            closure.run();
        }
    }
}
