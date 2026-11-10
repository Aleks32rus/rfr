package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.fixture.StaticUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.junit.annotation.ProbeTarget;
import io.student.rangiffler.junit.engine.ProbeEngine;
import io.student.rangiffler.junit.annotation.StaticUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Проверяет возврат пользователей общего пула через настоящий жизненный цикл Jupiter.
 */
@DisplayName("Пользователи общего пула через UsersQueueExtension")
final class UsersQueueExtensionTest {
    @Test
    @DisplayName("Четыре последовательных теста проходят на пуле из трёх пользователей")
    void fourTestsCannotExhaustThreeUsers() {
        EngineExecutionResults execution = execution(RepeatedScenario.class);
        List<String> users = users(execution, "acquired");
        assertThat(List.of(execution.testEvents().succeeded().count(), execution.allEvents().failed().count(),
                users.size(), users.stream().distinct().count(), users.getFirst().equals(users.getLast()), pool().stream().sorted().toList()))
                .as("Последовательные тесты не должны исчерпывать пул или создавать дополнительных пользователей")
                .isEqualTo(List.of(4L, 0L, 4, 3L, true, users.stream().distinct().sorted().toList()));
    }

    @Test
    @DisplayName("После упавшего теста следующий тест получает всех трёх пользователей")
    void failedTestCannotKeepBorrowedUser() {
        EngineExecutionResults execution = execution(FailureScenario.class);
        List<String> returned = users(execution, "returned");
        assertThat(List.of(execution.testEvents().succeeded().count(), execution.allEvents().failed().count(),
                execution.testEvents().failed().map(event -> event.getRequiredPayload(TestExecutionResult.class)
                        .getThrowable().orElseThrow().getMessage()).toList(),
                returned.size(), returned.stream().distinct().count(), returned.containsAll(users(execution, "failed")),
                users(execution, "failed").size(), pool().stream().sorted().toList()))
                .as("Упавший тест не должен удерживать пользователя или уменьшать доступную ёмкость пула")
                .isEqualTo(List.of(1L, 1L, List.of("Намеренное падение после получения пользователя пула"), 3, 3L, true, 1, returned.stream().sorted().toList()));
    }

    private List<String> pool() {
        try (UserFixture first = new StaticUserFixture(Duration.ofSeconds(2));
             UserFixture second = new StaticUserFixture(Duration.ofSeconds(2));
             UserFixture third = new StaticUserFixture(Duration.ofSeconds(2))) {
            return List.of(first.create().id().toString(), second.create().id().toString(), third.create().id().toString());
        }
    }

    private EngineExecutionResults execution(Class<?> scenario) {
        return EngineTestKit.engine(new ProbeEngine()).selectors(selectClass(scenario))
                .configurationParameter("rfr.probe.targets.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .configurationParameter("junit.jupiter.extensions.store.close.autocloseable.enabled", "true")
                .execute();
    }

    private List<String> users(EngineExecutionResults execution, String stage) {
        return execution.testEvents().reportingEntryPublished()
                .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
                .filter(entry -> stage.equals(entry.get("stage"))).map(entry -> entry.get("user")).toList();
    }

    /**
     * Повторяет получение пользователя в четырёх отдельных контекстах Store.
     */
    @DisplayName("Сценарий четырёх последовательных получений пользователя")
    @ProbeTarget
    static final class RepeatedScenario {
        @RepeatedTest(4)
        @DisplayName("Пользователь доступен очередному тесту")
        void poolCannotRunOutDuringSequentialTests(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("stage", "acquired", "user", user.id().toString()));
        }
    }

    /**
     * После падения требует всю ёмкость пула, чтобы обнаружить невозвращённого пользователя.
     */
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("Сценарий возврата пользователя после падения")
    @ProbeTarget
    static final class FailureScenario {
        @Test
        @Order(1)
        @DisplayName("Тест намеренно падает после получения пользователя")
        void failureCannotPreventStoreCleanup(TestReporter reporter, @StaticUser TestUser user) {
            reporter.publishEntry(Map.of("stage", "failed", "user", user.id().toString()));
            throw new AssertionError("Намеренное падение после получения пользователя пула");
        }

        @Test
        @Order(2)
        @DisplayName("Следующий тест получает весь пул, включая возвращённого пользователя")
        void nextTestCannotLosePoolCapacity(TestReporter reporter, @StaticUser TestUser first,
                                            @StaticUser TestUser second, @StaticUser TestUser third) {
            for (TestUser user : List.of(first, second, third)) {
                reporter.publishEntry(Map.of("stage", "returned", "user", user.id().toString()));
            }
        }
    }
}
