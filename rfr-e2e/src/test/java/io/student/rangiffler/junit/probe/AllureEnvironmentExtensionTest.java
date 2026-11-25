package io.student.rangiffler.junit.probe;

import io.student.rangiffler.junit.annotation.ProbeTarget;
import io.student.rangiffler.junit.extension.AllureEnvironmentExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Проверяет сведения об окружении, записанные после завершения запуска Jupiter.
 */
@DisplayName("Окружение запуска в отчёте Allure")
@Timeout(30)
final class AllureEnvironmentExtensionTest {
    @Test
    @DisplayName("Файл окружения содержит параметры запуска и версии обеих схем")
    void environmentHasAllKeys(@TempDir Path dir) throws IOException {
        EngineTestKit.engine("junit-jupiter").selectors(selectClass(Target.class))
                .configurationParameter("rfr.probe.targets.enabled", "true")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("allure.results.directory", dir.toString()).execute();
        Properties environment = new Properties();
        try (InputStream input = Files.newInputStream(dir.resolve("environment.properties"))) {
            environment.load(input);
        }
        assertThat(environment.stringPropertyNames())
                .as("В файле окружения не должны отсутствовать обязательные ключи")
                .contains("frontUrl", "authUrl", "apiUrl", "browser", "headless", "commit",
                        "rangiffler-auth", "rangiffler-api");
    }

    /**
     * Даёт Jupiter контейнер для выполнения хуков расширения.
     */
    @ProbeTarget
    @ExtendWith(AllureEnvironmentExtension.class)
    @DisplayName("Мишень записи окружения")
    static final class Target {
        @Test
        @DisplayName("Пустой тест для запуска расширения")
        void completes() {
        }
    }
}
