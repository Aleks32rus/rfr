package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.junit.annotation.User;
import io.student.rangiffler.junit.extension.UserExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Проверяет разрешение параметров и жизненный цикл фикстур через настоящий движок Jupiter и БД.
 */
@Tag("db")
@DisplayName("Подготовка и очистка пользователей через UserExtension")
final class UserExtensionTest {
    @Test
    @DisplayName("Расширение передаёт пользователя с аккаунтом, профилем и правами")
    void annotatedUserCannotReachTestWithoutAccountProfileOrAuthorities() {
        EngineExecutionResults execution = execution(SingleScenario.class);
        assertThat(List.of(execution.testEvents().succeeded().count(), entries(execution, "owner").stream()
                .map(entry -> List.of(entry.get("auth"), entry.get("profile"), entry.get("authorities"))).toList()))
                .as("Параметр пользователя не должен поступать в тест без аккаунта, профиля и прав")
                .isEqualTo(List.of(1L, List.of(List.of("true", "true", "true"))));
    }

    @Test
    @DisplayName("Расширение сохраняет количество друзей и направление приглашений")
    void annotatedRelationshipsCannotLoseCountsOrInvitationDirection() {
        EngineExecutionResults execution = execution(RelationshipScenario.class);
        assertThat(List.of(execution.testEvents().succeeded().count(), entries(execution, "relationships").stream()
                .map(entry -> List.of(entry.get("friends"), entry.get("income"), entry.get("accepted"),
                        entry.get("incoming"), entry.get("outgoing"))).toList()))
                .as("Параметры аннотации не должны терять связи или менять направление приглашений")
                .isEqualTo(List.of(1L, List.of(List.of("3", "2", "3", "2", "0"))));
    }

    @Test
    @DisplayName("Разные параметры теста получают разных пользователей")
    void differentParametersCannotShareUser() {
        EngineExecutionResults execution = execution(PairScenario.class);
        assertThat(List.of(execution.testEvents().succeeded().count(), entries(execution, "owner").size(),
                entries(execution, "owner").stream().map(entry -> entry.get("id")).distinct().count()))
                .as("Разные параметры одного теста не должны получать одного пользователя")
                .isEqualTo(List.of(1L, 2, 2L));
    }

    @Test
    @DisplayName("Каждый повтор теста получает отдельного пользователя")
    void repeatedInvocationsCannotShareUser() {
        EngineExecutionResults execution = execution(RepeatedScenario.class);
        assertThat(List.of(execution.testEvents().succeeded().count(), entries(execution, "owner").stream()
                .filter(entry -> entry.get("stage").equals("body")).map(entry -> entry.get("id")).distinct().count()))
                .as("Разные вызовы повторяемого теста не должны получать одного пользователя")
                .isEqualTo(List.of(3L, 3L));
    }

    @Test
    @DisplayName("Тест и AfterEach получают одну действующую фикстуру по индексу параметра")
    void resolvingSameIndexAfterTestCannotCreateAnotherFixture() {
        EngineExecutionResults execution = execution(RepeatedScenario.class);
        assertThat(List.of(execution.testEvents().succeeded().count(), entries(execution, "owner").stream()
                .map(entry -> entry.get("id")).toList(), entries(execution, "owner").stream()
                .allMatch(entry -> entry.get("auth").equals("true") && entry.get("profile").equals("true"))))
                .as("Повторное разрешение индекса в AfterEach не должно создавать или преждевременно очищать фикстуру")
                .isEqualTo(List.of(3L, entries(execution, "owner").stream()
                        .filter(entry -> entry.get("stage").equals("body"))
                        .flatMap(entry -> List.of(entry.get("id"), entry.get("id")).stream()).toList(), true));
    }

    @Test
    @DisplayName("После успешного теста все участники удаляются из обеих БД")
    void successfulTestCannotLeaveAnyParticipantInDatabases() {
        EngineExecutionResults execution = execution(RelationshipScenario.class);
        assertThat(List.of(execution.testEvents().succeeded().count(), users(execution).size(),
                users(execution).stream().anyMatch(this::remaining)))
                .as("Завершившийся тест не должен оставлять владельца, друзей или отправителей в БД")
                .isEqualTo(List.of(1L, 6, false));
    }

    @Test
    @DisplayName("После падения теста все участники удаляются из обеих БД")
    void failedTestCannotLeaveAnyParticipantInDatabases() {
        EngineExecutionResults execution = execution(FailureScenario.class);
        assertThat(List.of(execution.testEvents().failed().map(event -> event
                        .getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getMessage()).toList(),
                users(execution).size(), users(execution).stream().anyMatch(this::remaining)))
                .as("Упавший тест не должен оставлять владельца, друзей или отправителей в БД")
                .isEqualTo(List.of(List.of("Намеренное падение сценария после подготовки пользователя"), 4, false));
    }

    @Test
    @DisplayName("Ошибка второго параметра не мешает очистке первого пользователя")
    void failedParameterResolutionCannotLeaveEarlierUserInDatabases() {
        EngineExecutionResults execution = execution(PartialScenario.class);
        assertThat(List.of(failures(execution).stream().map(message -> message.contains(
                        "Количество друзей и входящих приглашений не может быть отрицательным")).toList(), users(execution).size(),
                entries(execution, "owner").stream().map(entry -> entry.get("stage")).toList(),
                users(execution).stream().anyMatch(this::remaining)))
                .as("Ошибка второго параметра не должна пропускать очистку ранее подготовленного пользователя")
                .isEqualTo(List.of(List.of(true), 1, List.of("before"), false));
    }

    @Test
    @DisplayName("Расширение отклоняет параметр без аннотации User")
    void extensionCannotResolveUserWithoutAnnotation() {
        EngineExecutionResults execution = execution(UnannotatedScenario.class);
        assertThat(failures(execution))
                .as("Расширение не должно передавать пользователя без аннотации User")
                .singleElement().asString().startsWith("No ParameterResolver registered for parameter");
    }

    @Test
    @DisplayName("Расширение отклоняет аннотированный параметр другого типа")
    void extensionCannotResolveAnnotatedParameterOfAnotherType() {
        EngineExecutionResults execution = execution(UnsupportedScenario.class);
        assertThat(failures(execution))
                .as("Расширение не должно разрешать аннотированный параметр другого типа")
                .singleElement().asString().startsWith("No ParameterResolver registered for parameter");
    }

    private EngineExecutionResults execution(Class<?> scenario) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(scenario))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .configurationParameter("junit.jupiter.extensions.store.close.autocloseable.enabled", "true")
                .execute();
    }

    private List<Map<String, String>> entries(EngineExecutionResults execution, String event) {
        return execution.testEvents().reportingEntryPublished()
                .map(report -> report.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
                .filter(entry -> event.equals(entry.get("event"))).toList();
    }

    private List<String> failures(EngineExecutionResults execution) {
        return execution.testEvents().failed().map(event -> event.getRequiredPayload(TestExecutionResult.class)
                .getThrowable().orElseThrow().getMessage()).toList();
    }

    private List<TestUser> users(EngineExecutionResults execution) {
        return execution.testEvents().reportingEntryPublished()
                .map(report -> report.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
                .filter(entry -> entry.containsKey("id"))
                .map(entry -> new TestUser(UUID.fromString(entry.get("id")), entry.get("username"), "unused-report-password"))
                .distinct().toList();
    }

    private boolean remaining(TestUser user) {
        return new AuthAccount(user).exist() || new Profile(user).exist()
                || new Authorities(user).exist() || new Friendships(user).exist();
    }

    /**
     * Публикует состояние участников для проверок после закрытия Store без передачи паролей.
     */
    private static final class Journal {
        private final TestReporter reporter;

        private Journal(TestReporter reporter) {
            this.reporter = reporter;
        }

        private void publish(String stage, TestUser user) {
            publish(stage, "owner", user);
            user.friends().forEach(friend -> publish(stage, "friend", friend));
            user.incomeInvitations().forEach(invitation -> publish(stage, "invitation", invitation));
            reporter.publishEntry(Map.of("event", "relationships", "stage", stage,
                    "friends", Integer.toString(user.friends().size()),
                    "income", Integer.toString(user.incomeInvitations().size()),
                    "accepted", Long.toString(new Friendships(user).count(FriendshipStatus.ACCEPTED)),
                    "incoming", Long.toString(new Friendships(user).incomeInvitations()),
                    "outgoing", Long.toString(new Friendships(user).outcomeInvitations())));
        }

        private void publish(String stage, String event, TestUser user) {
            reporter.publishEntry(Map.of("event", event, "stage", stage, "id", user.id().toString(),
                    "username", user.username(), "auth", Boolean.toString(new AuthAccount(user).exist()),
                    "profile", Boolean.toString(new Profile(user).exist()),
                    "authorities", Boolean.toString(new Authorities(user).exist())));
        }
    }

    /**
     * Получает пользователя через регистрацию расширения на аннотации параметра.
     */
    @DisplayName("Сценарий получения подготовленного пользователя")
    static final class SingleScenario {
        @Test
        @DisplayName("Получение подготовленного пользователя через аннотацию User")
        void userCannotBeUnprepared(TestReporter reporter, @User TestUser user) {
            new Journal(reporter).publish("body", user);
        }
    }

    /**
     * Получает пользователя с обоими видами связей из параметров аннотации.
     */
    @DisplayName("Сценарий получения пользователя со связями")
    static final class RelationshipScenario {
        @Test
        @DisplayName("Получение пользователя с друзьями и входящими приглашениями")
        void userCannotLoseRelationships(TestReporter reporter, @User(friends = 3, incomeInvitations = 2) TestUser user) {
            new Journal(reporter).publish("body", user);
        }
    }

    /**
     * Запрашивает двух независимых пользователей в одном контексте теста.
     */
    @DisplayName("Сценарий получения двух пользователей")
    static final class PairScenario {
        @Test
        @DisplayName("Получение двух независимых пользователей в одном тесте")
        void parametersCannotShareUser(TestReporter reporter, @User TestUser first, @User TestUser second) {
            new Journal(reporter).publish("body", first);
            new Journal(reporter).publish("body", second);
        }
    }

    /**
     * Повторно разрешает один индекс до закрытия контекста каждого вызова.
     */
    @DisplayName("Сценарий повторного разрешения параметра")
    static final class RepeatedScenario {
        @RepeatedTest(3)
        @DisplayName("Получение подготовленного пользователя через аннотацию User")
        void userCannotBeUnprepared(TestReporter reporter, @User TestUser user) {
            new Journal(reporter).publish("body", user);
        }

        @AfterEach
        void report(TestReporter reporter, @User TestUser user) {
            new Journal(reporter).publish("after", user);
        }
    }

    /**
     * Завершает тест ошибкой после успешной подготовки участников.
     */
    @DisplayName("Сценарий намеренного падения теста")
    static final class FailureScenario {
        @Test
        @DisplayName("Намеренное падение после подготовки пользователя и его связей")
        void failureCannotSkipCleanup(TestReporter reporter, @User(friends = 1, incomeInvitations = 2) TestUser user) {
            new Journal(reporter).publish("body", user);
            throw new AssertionError("Намеренное падение сценария после подготовки пользователя");
        }
    }

    /**
     * Сохраняет событие первого параметра перед ошибкой разрешения второго.
     */
    @DisplayName("Сценарий ошибки разрешения второго параметра")
    static final class PartialScenario {
        @BeforeEach
        void prepare(@User TestUser user, TestReporter reporter) {
            new Journal(reporter).publish("before", user);
        }

        @Test
        @DisplayName("Отказ второму параметру с отрицательным количеством друзей")
        void invalidParameterCannotReachBody(@User TestUser user, @User(friends = -7) TestUser invalid, TestReporter reporter) {
            reporter.publishEntry("event", "body");
        }
    }

    /**
     * Регистрирует расширение явно для проверки отказа параметру без аннотации.
     */
    @ExtendWith(UserExtension.class)
    @DisplayName("Сценарий параметра без аннотации User")
    static final class UnannotatedScenario {
        @Test
        @DisplayName("Отказ параметру TestUser без аннотации User")
        void unannotatedUserCannotReachBody(TestReporter reporter, TestUser user) {
            reporter.publishEntry("event", "body");
        }
    }

    /**
     * Проверяет ограничение расширения на тип аннотированного параметра.
     */
    @DisplayName("Сценарий параметра неподдерживаемого типа")
    static final class UnsupportedScenario {
        @Test
        @DisplayName("Отказ параметру String с аннотацией User")
        void unsupportedTypeCannotReachBody(TestReporter reporter, @User String user) {
            reporter.publishEntry("event", "body");
        }
    }
}