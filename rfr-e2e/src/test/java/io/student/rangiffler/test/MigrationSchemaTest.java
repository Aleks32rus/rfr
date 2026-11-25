package io.student.rangiffler.test;

import io.student.rangiffler.data.db.MigrationSchema;
import io.student.rangiffler.data.db.SchemaCompatibility;
import org.flywaydb.core.api.CoreErrorCode;
import org.flywaydb.core.api.ErrorDetails;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Проверяет отказ и действия пользователя по результатам штатной валидации Flyway без обращения к БД.
 */
@DisplayName("Соответствие схемы миграциям текущей ветки")
final class MigrationSchemaTest {
    @Test
    @DisplayName("Совпадающая история миграций не останавливает запуск")
    void matchingHistoryCannotBlockTests() {
        SchemaCompatibility schema = new MigrationSchema("archive_73", "rfr-api", () -> result(true, 17));
        assertThatCode(schema::verify)
                .as("Совпадающая история не должна блокировать запуск тестов")
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = CoreErrorCode.class, names = {"CHECKSUM_MISMATCH", "TYPE_MISMATCH", "DESCRIPTION_MISMATCH"})
    @DisplayName("Изменённая применённая миграция требует исходный файл или совместимую БД")
    void changedMigrationCannotPassValidation(CoreErrorCode code) {
        SchemaCompatibility schema = new MigrationSchema("archive_73", "rfr-api", () -> result(false, 17,
                new ValidateOutput("7.19", "geography", "V7_19__geography.sql", new ErrorDetails(code, "Mismatch"))));
        String message = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow().getMessage();
        assertThat(message)
                .as("Изменённая миграция не должна проходить проверку без указания схемы, версии и действия")
                .contains("archive_73", "V7.19", code.toString(), "верните исходный файл миграции", "восстановите локальную БД");
    }

    @Test
    @DisplayName("Лишняя применённая миграция требует совместимую ветку или восстановление БД")
    void databaseAheadOfBranchCannotPassValidation() {
        SchemaCompatibility schema = new MigrationSchema("archive_73", "rfr-api", () -> result(false, 17,
                new ValidateOutput("31.7", "another branch", "V31_7__another_branch.sql",
                        new ErrorDetails(CoreErrorCode.APPLIED_VERSIONED_MIGRATION_NOT_RESOLVED, "Future migration"))));
        String message = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow().getMessage();
        assertThat(message)
                .as("Миграция другой ветки не должна проходить проверку без понятного способа восстановления совместимости")
                .contains("archive_73", "V31.7", "отсутствует в текущей ветке", "переключитесь на совместимую ветку",
                        "восстановите локальную БД");
    }

    @Test
    @DisplayName("Неприменённая миграция предлагает запустить соответствующий бэкенд")
    void pendingMigrationCannotPassWithoutBackendAction() {
        SchemaCompatibility schema = new MigrationSchema("identity_59", "rfr-auth", () -> result(false, 23,
                new ValidateOutput("9.43", "new permissions", "V9_43__permissions.sql",
                        new ErrorDetails(CoreErrorCode.RESOLVED_VERSIONED_MIGRATION_NOT_APPLIED, "Pending migration"))));
        String message = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow().getMessage();
        assertThat(message)
                .as("Неприменённая миграция не должна оставлять пользователя без команды запуска нужного бэкенда")
                .contains("identity_59", "V9.43", "не применены", ".\\gradlew.bat :rfr-auth:bootRun", "повторите тесты");
    }

    @Test
    @DisplayName("Устаревшая повторяемая миграция указывается по имени без номера версии")
    void repeatableMigrationCannotLoseItsName() {
        SchemaCompatibility schema = new MigrationSchema("archive_73", "rfr-api", () -> result(false, 17,
                new ValidateOutput("", "country view", "R__country_view.sql",
                        new ErrorDetails(CoreErrorCode.OUTDATED_REPEATABLE_MIGRATION, "Outdated migration"))));
        String message = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow().getMessage();
        assertThat(message)
                .as("Повторяемая миграция не должна терять имя или действие по повторному применению")
                .contains("повторяемая миграция country view", "OUTDATED_REPEATABLE_MIGRATION", ":rfr-api:bootRun");
    }

    @Test
    @DisplayName("Ошибка уровня схемы диагностируется даже без списка отдельных миграций")
    void schemaFailureCannotLoseRecoveryAction() {
        SchemaCompatibility schema = new MigrationSchema("identity_59", "rfr-auth", () -> new ValidateResult(
                "13.7.0", "identity_59", new ErrorDetails(CoreErrorCode.SCHEMA_DOES_NOT_EXIST, "Missing schema"),
                false, 0, List.of(), List.of()));
        String message = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow().getMessage();
        assertThat(message)
                .as("Ошибка схемы не должна превращаться в пустое сообщение без способа восстановления")
                .contains("identity_59", "SCHEMA_DOES_NOT_EXIST", ":rfr-auth:bootRun");
    }

    @Test
    @DisplayName("Пустой набор миграций бэкенда не считается успешной проверкой")
    void missingBackendResourcesCannotPassValidation() {
        SchemaCompatibility schema = new MigrationSchema("archive_73", "rfr-api", () -> result(true, 0));
        String message = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow().getMessage();
        assertThat(message)
                .as("Пустой набор ресурсов не должен выдавать отсутствие проверки за успешную совместимость")
                .contains("archive_73", "не найдены миграции", "rfr-api", ":rfr-e2e:processTestResources");
    }

    @Test
    @DisplayName("Сбой Flyway предлагает проверить MySQL и настройки подключения")
    void unavailableDatabaseCannotLoseConnectionAction() {
        SchemaCompatibility schema = new MigrationSchema("identity_59", "rfr-auth", () -> {
            throw new FlywayException("Connection failed");
        });
        String message = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow().getMessage();
        assertThat(message)
                .as("Недоступная БД не должна оставлять пользователя без действия для проверки окружения")
                .contains("identity_59", "localenv.sh", "настройки подключения", "rfr-auth", "повторите тесты");
    }

    @Test
    @DisplayName("Сбой Flyway сохраняется как причина ошибки проверки")
    void flywayFailureCannotLoseOriginalCause() {
        FlywayException failure = new FlywayException("Native failure 47");
        SchemaCompatibility schema = new MigrationSchema("archive_73", "rfr-api", () -> {
            throw failure;
        });
        Throwable result = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow();
        assertThat(result)
                .as("Ошибка проверки не должна скрывать исходную причину сбоя Flyway")
                .hasCause(failure);
    }

    @Test
    @DisplayName("Несколько расхождений одной схемы сохраняются в сообщении")
    void severalMismatchesCannotHideEachOther() {
        SchemaCompatibility schema = new MigrationSchema("archive_73", "rfr-api", () -> result(false, 17,
                new ValidateOutput("5.31", "removed", "V5_31__removed.sql",
                        new ErrorDetails(CoreErrorCode.APPLIED_VERSIONED_MIGRATION_NOT_RESOLVED, "Missing migration")),
                new ValidateOutput("8.17", "changed", "V8_17__changed.sql",
                        new ErrorDetails(CoreErrorCode.CHECKSUM_MISMATCH, "Changed migration"))));
        String message = Optional.ofNullable(catchThrowable(schema::verify)).orElseThrow().getMessage();
        assertThat(message)
                .as("Несколько расхождений одной схемы не должны скрывать версии или причины друг друга")
                .contains("V5.31", "APPLIED_VERSIONED_MIGRATION_NOT_RESOLVED", "V8.17", "CHECKSUM_MISMATCH");
    }

    private ValidateResult result(boolean success, int count, ValidateOutput... migrations) {
        return new ValidateResult("13.7.0", "probe_67", new ErrorDetails(CoreErrorCode.VALIDATE_ERROR, "Probe result"),
                success, count, Arrays.asList(migrations), List.of());
    }
}
