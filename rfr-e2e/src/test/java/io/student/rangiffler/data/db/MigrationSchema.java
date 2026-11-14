package io.student.rangiffler.data.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Сравнивает историю Flyway с миграциями ветки и объясняет способ устранения расхождения.
 */
public final class MigrationSchema implements SchemaCompatibility {
    private final String schema;
    private final String backend;
    private final Supplier<ValidateResult> validation;

    public MigrationSchema(String schema, String backend, Supplier<ValidateResult> validation) {
        this.schema = schema;
        this.backend = backend;
        this.validation = validation;
    }

    public MigrationSchema(DataSource database, String schema, String backend) {
        this(schema, backend, () -> Flyway.configure().dataSource(database).schemas(schema)
                .locations("classpath:db/migration/" + schema).ignoreMigrationPatterns(new String[0])
                .createSchemas(false).cleanDisabled(true).skipDefaultCallbacks(true)
                .validateMigrationNaming(true).failOnMissingLocations(true).connectRetries(0)
                .load().validateWithResult());
    }

    @Override
    public void verify() {
        ValidateResult result;
        try {
            result = validation.get();
        } catch (FlywayException failure) {
            throw new IllegalStateException("Не удалось проверить миграции БД " + schema
                    + "; запустите MySQL через localenv.sh, проверьте настройки подключения и миграции "
                    + backend + ", затем повторите тесты", failure);
        }
        if (result.validationSuccessful) {
            if (result.validateCount == 0) {
                throw new IllegalStateException("Для БД " + schema + " не найдены миграции бэкенда " + backend
                        + "; проверьте миграции текущей ветки и выполните .\\gradlew.bat :rfr-e2e:processTestResources");
            }
            return;
        }
        List<ValidateOutput> migrations = Optional.ofNullable(result.invalidMigrations).orElseGet(List::of);
        throw new IllegalStateException("Миграции БД " + schema + " не соответствуют текущей ветке: "
                + (migrations.isEmpty() ? diagnostic(Optional.ofNullable(result.errorDetails)
                .map(details -> details.errorCode.toString()).orElse("VALIDATE_ERROR"))
                : migrations.stream().map(this::migration).collect(Collectors.joining("; "))));
    }

    private String migration(ValidateOutput migration) {
        return Optional.ofNullable(migration.version).filter(version -> !version.isBlank())
                .map(version -> "V" + version).orElseGet(() -> "повторяемая миграция "
                        + Optional.ofNullable(migration.description).orElse("без имени"))
                + " — " + diagnostic(migration.errorDetails.errorCode.toString());
    }

    private String diagnostic(String code) {
        return switch (code) {
            case "RESOLVED_VERSIONED_MIGRATION_NOT_APPLIED", "RESOLVED_REPEATABLE_MIGRATION_NOT_APPLIED",
                 "OUTDATED_REPEATABLE_MIGRATION", "SCHEMA_DOES_NOT_EXIST" ->
                    "миграции текущей ветки не применены [" + code + "]; запустите .\\gradlew.bat :" + backend
                            + ":bootRun и дождитесь успешного выполнения Flyway, затем повторите тесты"
                            + "; если история относится к другой ветке, восстановите совместимую локальную БД";
            case "APPLIED_VERSIONED_MIGRATION_NOT_RESOLVED", "APPLIED_REPEATABLE_MIGRATION_NOT_RESOLVED" ->
                    "применена в БД, но отсутствует в текущей ветке [" + code + "]; переключитесь на совместимую ветку"
                            + " либо восстановите локальную БД для текущей ветки, затем запустите " + backend + " и повторите тесты";
            case "CHECKSUM_MISMATCH", "TYPE_MISMATCH", "DESCRIPTION_MISMATCH" ->
                    "содержимое, тип или описание отличается от применённой миграции [" + code
                            + "]; верните исходный файл миграции либо восстановите локальную БД для текущей ветки"
                            + ", затем запустите " + backend + " и повторите тесты";
            default -> "история миграций несовместима с текущей веткой [" + code
                    + "]; проверьте журнал Flyway бэкенда " + backend
                    + " и восстановите совместимую локальную БД перед повторным запуском тестов";
        };
    }
}
