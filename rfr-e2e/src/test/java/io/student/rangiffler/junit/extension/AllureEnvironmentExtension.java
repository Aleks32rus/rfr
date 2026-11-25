package io.student.rangiffler.junit.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Migrations;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Сохраняет сведения об окружении запуска для отчёта Allure.
 */
public final class AllureEnvironmentExtension implements SuiteExtension {
    @Override
    public void afterSuite(ExtensionContext context) {
        Path directory = Path.of(context.getConfigurationParameter("allure.results.directory")
                .orElse("build/allure-results"));
        Properties environment = new Properties();
        environment.setProperty("frontUrl", TestConfig.FRONT_URL);
        environment.setProperty("authUrl", TestConfig.AUTH_URL);
        environment.setProperty("apiUrl", TestConfig.API_URL);
        environment.setProperty("browser", "chromium");
        environment.setProperty("headless", "true");
        environment.setProperty("commit", TestConfig.commit());
        for (Databases database : Databases.values()) {
            environment.setProperty(database.schema(), new Migrations.FlywayHistory(database).version());
        }
        try {
            Files.createDirectories(directory);
            try (OutputStream output = Files.newOutputStream(directory.resolve("environment.properties"))) {
                environment.store(output, "Allure environment");
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Не удалось записать environment.properties в " + directory, failure);
        }
    }
}
