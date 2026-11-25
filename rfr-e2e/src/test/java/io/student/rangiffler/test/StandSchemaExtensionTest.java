package io.student.rangiffler.test;

import io.student.rangiffler.data.db.SchemaCompatibility;
import io.student.rangiffler.junit.extension.StandSchemaExtension;
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
 * Проверяет обязательную проверку обеих схем и сохранение причин общего отказа запуска.
 */
@ExtendWith(StandSchemaExtensionTest.CurrentContext.class)
@DisplayName("Проверка миграций перед общим запуском")
final class StandSchemaExtensionTest {
    @Test
    @DisplayName("Перед тестами проверяются обе переданные схемы")
    void suiteCannotSkipEitherSchema(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        new StandSchemaExtension(List.of(new ObservedSchema(events, "auth_47", () -> {
                }),
                new ObservedSchema(events, "api_83", () -> {
                }))).beforeSuite(context);
        assertThat(events)
                .as("Проверка запуска не должна пропускать одну из переданных схем")
                .containsExactly("auth_47", "api_83");
    }

    @Test
    @DisplayName("Ошибка первой схемы не отменяет проверку второй")
    void firstSchemaFailureCannotSkipSecondSchema(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        StandSchemaExtension extension = new StandSchemaExtension(List.of(new ObservedSchema(events, "auth_47", () -> {
            throw new IllegalStateException("Верните совместимую схему auth_47");
        }), new ObservedSchema(events, "api_83", () -> {
        })));
        Optional<Throwable> failure = Optional.ofNullable(catchThrowable(() -> extension.beforeSuite(context)));
        assertThat(List.of(events, failure.isPresent()))
                .as("Ошибка первой схемы не должна отменять проверку второй или разрешать выполнение тестов")
                .isEqualTo(List.of(List.of("auth_47", "api_83"), true));
    }

    @Test
    @DisplayName("Пользователь получает причины расхождения обеих схем")
    void bothSchemaFailuresCannotLoseRecoveryActions(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        StandSchemaExtension extension = new StandSchemaExtension(List.of(new ObservedSchema(events, "auth_47", () -> {
            throw new IllegalStateException("Запустите rfr-auth для auth_47");
        }), new ObservedSchema(events, "api_83", () -> {
            throw new IllegalStateException("Восстановите api_83 для текущей ветки");
        })));
        String message = Optional.ofNullable(catchThrowable(() -> extension.beforeSuite(context))).orElseThrow().getMessage();
        assertThat(message)
                .as("Расхождения двух схем не должны скрывать друг друга или терять действия для пользователя")
                .contains("Запустите rfr-auth для auth_47", "Восстановите api_83 для текущей ветки");
    }

    @Test
    @DisplayName("Оригинальные ошибки обеих схем сохраняются в suppressed")
    void schemaFailuresCannotLoseOriginalExceptions(ExtensionContext context) {
        List<String> events = new ArrayList<>();
        IllegalStateException auth = new IllegalStateException("Несовместима auth_47");
        IllegalStateException api = new IllegalStateException("Несовместима api_83");
        StandSchemaExtension extension = new StandSchemaExtension(List.of(new ObservedSchema(events, "auth_47", () -> {
            throw auth;
        }), new ObservedSchema(events, "api_83", () -> {
            throw api;
        })));
        Throwable failure = Optional.ofNullable(catchThrowable(() -> extension.beforeSuite(context))).orElseThrow();
        assertThat(Arrays.asList(failure.getSuppressed()))
                .as("Общая ошибка запуска не должна терять исходные исключения отдельных схем")
                .containsExactly(auth, api);
    }

    /**
     * Передаёт настоящий контекст Jupiter без обращения к инфраструктуре приложения.
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
     * Фиксирует обращение к схеме и задаёт независимый результат её проверки.
     */
    private static final class ObservedSchema implements SchemaCompatibility {
        private final List<String> events;
        private final String name;
        private final Runnable validation;

        private ObservedSchema(List<String> events, String name, Runnable validation) {
            this.events = events;
            this.name = name;
            this.validation = validation;
        }

        @Override
        public void verify() {
            events.add(name);
            validation.run();
        }
    }
}
