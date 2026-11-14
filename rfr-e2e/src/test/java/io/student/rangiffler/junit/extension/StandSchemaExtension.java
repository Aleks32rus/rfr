package io.student.rangiffler.junit.extension;

import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.MigrationSchema;
import io.student.rangiffler.data.db.SchemaCompatibility;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Останавливает общий запуск до тестов при несовместимости миграций auth или API.
 */
public final class StandSchemaExtension implements SuiteExtension {
    private final List<SchemaCompatibility> schemas;

    public StandSchemaExtension(List<SchemaCompatibility> schemas) {
        this.schemas = List.copyOf(schemas);
    }

    public StandSchemaExtension() {
        this(List.of(new MigrationSchema(Databases.AUTH.dataSource(), "rangiffler-auth", "rfr-auth"),
                new MigrationSchema(Databases.API.dataSource(), "rangiffler-api", "rfr-api")));
    }

    @Override
    public void beforeSuite(ExtensionContext context) {
        List<IllegalStateException> failures = new ArrayList<>();
        for (SchemaCompatibility schema : schemas) {
            try {
                schema.verify();
            } catch (IllegalStateException failure) {
                failures.add(failure);
            }
        }
        if (!failures.isEmpty()) {
            IllegalStateException failure = new IllegalStateException("Проверка схем перед тестами не пройдена: "
                    + failures.stream().map(Throwable::getMessage).collect(Collectors.joining("; ")));
            failures.forEach(failure::addSuppressed);
            throw failure;
        }
    }
}
