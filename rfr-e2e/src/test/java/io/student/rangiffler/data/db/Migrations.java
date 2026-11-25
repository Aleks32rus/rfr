package io.student.rangiffler.data.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;

import java.util.Optional;

/**
 * Предоставляет версию применённых миграций схемы.
 */
public interface Migrations {
    String version();

    /**
     * Читает последнюю применённую версию из истории Flyway.
     */
    final class FlywayHistory implements Migrations {
        private final Databases database;

        public FlywayHistory(Databases database) {
            this.database = database;
        }

        @Override
        public String version() {
            try {
                return Optional.ofNullable(Flyway.configure().dataSource(database.dataSource())
                                .schemas(database.schema()).locations("classpath:db/migration/" + database.schema())
                                .createSchemas(false).skipDefaultCallbacks(true).connectRetries(0).load().info().current())
                        .map(MigrationInfo::getVersion).map(MigrationVersion::getVersion).orElse("unavailable");
            } catch (FlywayException failure) {
                return "unavailable";
            }
        }
    }
}
