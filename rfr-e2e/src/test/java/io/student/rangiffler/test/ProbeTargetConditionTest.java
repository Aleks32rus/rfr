package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.junit.annotation.ProbeTarget;
import io.student.rangiffler.junit.annotation.StaticUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Проверяет изоляцию мишеней при обычном запуске с глобальным разрешением JVM.
 */
@Timeout(value = 15, unit = TimeUnit.SECONDS)
@DisplayName("Изоляция мишеней от обычного Launcher")
final class ProbeTargetConditionTest {
    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    @DisplayName("Глобальное свойство JVM не разрешает мишень обычному Launcher")
    void ordinaryLauncherCannotEnableProbeBySystemProperty() {
        Optional<String> property = Optional.ofNullable(System.getProperty("rfr.probe.targets.enabled"));
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        try {
            System.setProperty("rfr.probe.targets.enabled", "true");
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(Target.class)).build(), listener);
        } finally {
            if (property.isPresent()) {
                System.setProperty("rfr.probe.targets.enabled", property.orElseThrow());
            } else {
                System.clearProperty("rfr.probe.targets.enabled");
            }
        }
        assertThat(List.of(listener.getSummary().getTestsStartedCount(),
                listener.getSummary().getContainersSkippedCount(), listener.getSummary().getTotalFailureCount()))
                .as("Обычный Launcher не должен запускать мишень или получать пользователя из-за свойства JVM")
                .isEqualTo(List.of(0L, 1L, 0L));
    }

    /**
     * Позволяет обнаружить нежелательное получение пользователя без зависимости от БД.
     */
    @ProbeTarget
    @DisplayName("Мишень получения пользователя общего пула")
    static final class Target {
        @Test
        @DisplayName("Получение пользователя и выполнение тела мишени")
        void targetCannotRunOutsideProbeEngine(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry("user", user.id().toString());
        }
    }
}
