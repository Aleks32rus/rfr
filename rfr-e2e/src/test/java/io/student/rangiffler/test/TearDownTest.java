package io.student.rangiffler.test;

import io.student.rangiffler.fixture.TearDown;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяет отложенную очистку, неизменяемость цепочки и сохранение ошибок.
 */
@DisplayName("Цепочка действий очистки")
final class TearDownTest {
    @Test
    @DisplayName("Построение цепочки не запускает очистку")
    void buildingCannotExecuteCleanup() {
        List<Integer> history = new ArrayList<>();
        new TearDown(List.of()).with(() -> history.add(17)).withEach(List.of(43, 29), history::add);
        assertEquals(List.of(), history, "Построение цепочки не должно запускать очистку");
    }

    @Test
    @DisplayName("Добавление действий сохраняет исходную цепочку очистки")
    void appendingCannotChangeOriginalChain() {
        List<Integer> history = new ArrayList<>();
        TearDown original = new TearDown(List.of(() -> history.add(17)));
        original.with(() -> history.add(83)).withEach(List.of(43, 29), history::add);
        original.close();
        assertEquals(List.of(17), history, "Добавление действий не должно менять исходную цепочку");
    }

    @Test
    @DisplayName("Очистка выполняет действия в порядке регистрации")
    void cleanupCannotChangeRegistrationOrder() {
        List<Integer> history = new ArrayList<>();
        new TearDown(List.of(() -> history.add(17))).withEach(List.of(43, 29), history::add)
                .with(() -> history.add(71)).close();
        assertEquals(List.of(17, 43, 29, 71), history, "Очистка не должна менять порядок регистрации действий");
    }

    @Test
    @DisplayName("Изменения коллекции не влияют на построенную цепочку очистки")
    void eachCannotObserveLaterCollectionChanges() {
        List<Integer> history = new ArrayList<>();
        List<Integer> values = new ArrayList<>(List.of(43, 29));
        TearDown cleanup = new TearDown(List.of()).withEach(values, history::add);
        values.add(71);
        cleanup.close();
        assertEquals(List.of(43, 29), history, "Изменение коллекции не должно менять уже построенную очистку");
    }

    @Test
    @DisplayName("Ошибка не прерывает остальные действия очистки и сохраняет дополнительные ошибки")
    void failureCannotSkipRemainingActionsOrLoseSuppressedErrors() {
        List<String> history = new ArrayList<>();
        try (TearDown cleanup = new TearDown(List.of())
                .with(() -> {
                    history.add("first");
                    throw new IllegalStateException("Не удалось очистить первый ресурс");
                })
                .with(() -> {
                    history.add("second");
                    throw new IllegalArgumentException("Не удалось очистить второй ресурс");
                })
                .with(() -> history.add("third"))) {
            history.add("body");
        } catch (IllegalStateException failure) {
            history.add(failure.getMessage());
            for (Throwable exception : failure.getSuppressed()) {
                history.add(exception.getMessage());
            }
        }
        assertEquals(List.of("body", "first", "second", "third", "Не удалось очистить первый ресурс",
                        "Не удалось очистить второй ресурс"), history,
                "Ошибка не должна пропускать оставшиеся действия или скрывать дополнительные ошибки");
    }

    @Test
    @DisplayName("Повторная ошибка не добавляется к самой себе как подавленная")
    void repeatedExceptionCannotSuppressItself() {
        IllegalStateException failure = new IllegalStateException("Не удалось очистить общий ресурс");
        List<RuntimeException> failures = new ArrayList<>();
        try (TearDown cleanup = new TearDown(List.of())
                .with(() -> {
                    throw failure;
                })
                .with(() -> {
                    throw failure;
                })) {
        } catch (RuntimeException exception) {
            failures.add(exception);
        }
        assertEquals(List.of(failure), failures, "Повторная ошибка не должна подавлять саму себя");
    }
}
