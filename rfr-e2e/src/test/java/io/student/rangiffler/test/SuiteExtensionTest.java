package io.student.rangiffler.test;

import io.student.rangiffler.junit.annotation.ProbeTarget;
import io.student.rangiffler.junit.engine.ProbeEngine;
import io.student.rangiffler.junit.extension.SuiteExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Проверяет контракт SuiteExtension только через вызовы его хуков настоящим движком Jupiter.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
@DisplayName("Контракт расширения уровня всего запуска")
final class SuiteExtensionTest {
    @Test
    @DisplayName("Несколько классов и повторов имеют одну подготовку и одно закрытие после всех тестов")
    void severalClassesCannotRepeatSuiteLifecycle() {
        EngineExecutionResults execution = execution(Map.of(), FirstProbe.class, SecondProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().succeeded().count(),
                execution.allEvents().failed().count(), owners(execution, "primary").stream().distinct().count()))
                .as("Классы и повторы не должны повторять подготовку или закрывать ресурс до окончания тестов")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body", "body", "body", "body",
                        "after", "resource-released"), 4L, 0L, 1L));
    }

    @Test
    @DisplayName("Ресурс запуска остаётся открытым до окончания класса без регистрации расширения")
    void unregisteredClassCannotRunAfterSuiteClosure() {
        EngineExecutionResults execution = execution(Map.of(), FirstProbe.class, UnextendedProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().succeeded().count(),
                execution.allEvents().failed().count()))
                .as("Закрытие запуска не должно происходить до выполнения последнего класса без регистрации расширения")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body", "body", "body", "after",
                        "resource-released"), 3L, 0L));
    }

    @Test
    @DisplayName("Три одновременно выполняемых класса имеют один общий жизненный цикл")
    void concurrentClassesCannotDuplicateSuiteLifecycle() {
        EngineExecutionResults execution = execution(Map.of("junit.jupiter.execution.parallel.enabled", "true"),
                ParallelFirstProbe.class, ParallelSecondProbe.class, ParallelThirdProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().succeeded().count(),
                execution.allEvents().failed().count(), entries(execution).stream()
                        .filter(entry -> entry.get("stage").equals("body-start"))
                        .map(entry -> entry.get("thread")).distinct().count(),
                owners(execution, "primary").stream().distinct().count()))
                .as("Одновременные классы не должны создавать несколько циклов или закрывать ресурс при работающем тесте")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body-start", "body-start", "body-start",
                        "body", "body", "body", "after", "resource-released"), 3L, 0L, 3L, 1L));
    }

    @Test
    @DisplayName("Падение теста не отменяет закрытие ресурса запуска")
    void failingTestCannotSkipSuiteClosure() {
        EngineExecutionResults execution = execution(Map.of(), FailingBodyProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().failed().count(),
                failures(execution).contains("Намеренное падение тела пробы")))
                .as("Падение теста не должно отменять afterSuite и освобождение ресурса запуска")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body", "after", "resource-released"),
                        1L, true));
    }

    @Test
    @DisplayName("Ошибка подготовки всё равно приводит к закрытию частично полученного ресурса")
    void beforeFailureCannotSkipPartialResourceClosure() {
        EngineExecutionResults execution = execution(Map.of("rfr.suite.probe.before.failure", "true"), FirstProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().started().count(),
                failures(execution).contains("Намеренная ошибка beforeSuite пробы")))
                .as("Ошибка beforeSuite не должна запускать тесты или оставлять частично полученный ресурс без закрытия")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "before-error", "after", "resource-released"),
                        0L, true));
    }

    @Test
    @DisplayName("Следующие классы не повторяют неуспешную подготовку и не выполняют тесты")
    void beforeFailureCannotRetryInitializationForAnotherClass() {
        EngineExecutionResults execution = execution(Map.of("rfr.suite.probe.before.failure", "true"),
                FirstProbe.class, SecondProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().started().count(),
                failures(execution).contains("Намеренная ошибка beforeSuite пробы")))
                .as("Другой класс не должен повторять упавшую подготовку или выполнять тесты после неё")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "before-error", "after", "resource-released"),
                        0L, true));
    }

    @Test
    @DisplayName("Параллельные классы не повторяют упавшую подготовку и закрывают ресурс один раз")
    void concurrentBeforeFailureCannotRetryOrSkipClosure() {
        EngineExecutionResults execution = execution(Map.of("rfr.suite.probe.before.failure", "true",
                        "junit.jupiter.execution.parallel.enabled", "true"),
                ParallelFirstProbe.class, ParallelSecondProbe.class, ParallelThirdProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().started().count(),
                failures(execution).contains("Намеренная ошибка beforeSuite пробы")))
                .as("Параллельные классы не должны повторять ошибочную подготовку или пропускать обязательное закрытие")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "before-error", "after", "resource-released"),
                        0L, true));
    }

    @Test
    @DisplayName("Ошибка закрытия видна в результате и не вызывает повторного закрытия")
    void afterFailureCannotDisappearOrRepeatClosure() {
        EngineExecutionResults execution = execution(Map.of("rfr.suite.probe.after.failure", "true"),
                FirstProbe.class, SecondProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().succeeded().count(),
                failures(execution).contains("Намеренная ошибка afterSuite пробы")))
                .as("Ошибка afterSuite не должна теряться или вызывать повторное закрытие ресурса")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body", "body", "body", "body",
                        "after", "after-error", "resource-released"), 4L, true));
    }

    @Test
    @DisplayName("Ошибка одного расширения не мешает закрытию ресурса другого расширения")
    void afterFailureCannotPreventAnotherExtensionFromClosing() {
        EngineExecutionResults execution = execution(Map.of("rfr.suite.probe.after.failure", "true"),
                CompanionProbe.class);
        assertThat(List.of(stages(execution, "primary"), stages(execution, "companion"),
                execution.testEvents().succeeded().count(),
                failures(execution).contains("Намеренная ошибка afterSuite пробы")))
                .as("Ошибка закрытия одного расширения не должна отменять закрытие другого")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body", "body", "after", "after-error",
                                "resource-released"),
                        List.of("before", "resource-acquired", "after", "resource-released"), 2L, true));
    }

    @Test
    @DisplayName("Обратный порядок регистрации также сохраняет закрытие другого расширения после ошибки")
    void afterFailureCannotSkipEarlierRegisteredExtensionClosure() {
        EngineExecutionResults execution = execution(Map.of("rfr.suite.probe.after.failure", "true"),
                CompanionFirstProbe.class);
        assertThat(List.of(stages(execution, "primary"), stages(execution, "companion"),
                execution.testEvents().succeeded().count(),
                failures(execution).contains("Намеренная ошибка afterSuite пробы")))
                .as("Обратный порядок регистрации не должен позволять ошибке одного расширения отменять закрытие другого")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body", "body", "after", "after-error",
                                "resource-released"),
                        List.of("before", "resource-acquired", "after", "resource-released"), 2L, true));
    }

    @Test
    @DisplayName("Одновременные ошибки подготовки и закрытия не скрывают друг друга")
    void beforeAndAfterFailuresCannotHideEachOther() {
        EngineExecutionResults execution = execution(Map.of("rfr.suite.probe.before.failure", "true",
                "rfr.suite.probe.after.failure", "true"), FirstProbe.class);
        assertThat(List.of(stages(execution, "primary"), execution.testEvents().started().count(),
                failures(execution).contains("Намеренная ошибка beforeSuite пробы"),
                failures(execution).contains("Намеренная ошибка afterSuite пробы")))
                .as("Ошибки beforeSuite и afterSuite не должны скрывать друг друга или отменять освобождение ресурса")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "before-error", "after", "after-error",
                        "resource-released"), 0L, true, true));
    }

    @Test
    @DisplayName("Разные реализации расширения имеют независимую подготовку и закрытие")
    void differentExtensionsCannotShareLifecycle() {
        EngineExecutionResults execution = execution(Map.of(), CompanionProbe.class);
        assertThat(List.of(stages(execution, "primary"), stages(execution, "companion"),
                execution.testEvents().succeeded().count(), execution.allEvents().failed().count(),
                entries(execution).stream().filter(entry -> entry.get("stage").equals("before"))
                        .map(entry -> entry.get("owner")).distinct().count()))
                .as("Разные реализации не должны пропускать собственные хуки или делить владельца ресурса")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body", "body", "after", "resource-released"),
                        List.of("before", "resource-acquired", "after", "resource-released"), 2L, 0L, 2L));
    }

    @Test
    @DisplayName("Два запуска движка в одной JVM имеют независимые жизненные циклы")
    void separateRunsCannotReusePreviousLifecycle() {
        EngineExecutionResults first = execution(Map.of(), FirstProbe.class);
        EngineExecutionResults second = execution(Map.of(), FirstProbe.class);
        assertThat(List.of(stages(first, "primary"), stages(second, "primary"),
                first.testEvents().succeeded().count(), second.testEvents().succeeded().count(),
                first.allEvents().failed().count(), second.allEvents().failed().count(),
                Stream.concat(owners(first, "primary").stream(), owners(second, "primary").stream()).distinct().count()))
                .as("Новый запуск движка не должен пропускать подготовку или повторно закрывать ресурс предыдущего запуска")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "body", "body", "after", "resource-released"),
                        List.of("before", "resource-acquired", "body", "body", "after", "resource-released"),
                        2L, 2L, 0L, 0L, 2L));
    }

    @Test
    @DisplayName("Неуспешная подготовка одного запуска не повреждает жизненный цикл следующего")
    void failedRunCannotPoisonFollowingRun() {
        EngineExecutionResults first = execution(Map.of("rfr.suite.probe.before.failure", "true"), FirstProbe.class);
        EngineExecutionResults second = execution(Map.of(), FirstProbe.class);
        assertThat(List.of(stages(first, "primary"), stages(second, "primary"),
                first.testEvents().started().count(), second.testEvents().succeeded().count(),
                failures(first).contains("Намеренная ошибка beforeSuite пробы"), second.allEvents().failed().count(),
                Stream.concat(owners(first, "primary").stream(), owners(second, "primary").stream()).distinct().count()))
                .as("Ошибка первого запуска не должна отменять его закрытие или мешать новому независимому запуску")
                .isEqualTo(List.of(List.of("before", "resource-acquired", "before-error", "after", "resource-released"),
                        List.of("before", "resource-acquired", "body", "body", "after", "resource-released"),
                        0L, 2L, true, 0L, 2L));
    }

    @Test
    @DisplayName("Обычный Jupiter не запускает пробы даже с разрешающим параметром")
    void ordinaryEngineCannotRunSuiteProbes() {
        EngineExecutionResults execution = EngineTestKit.engine("junit-jupiter").selectors(selectClass(FirstProbe.class))
                .configurationParameter("rfr.probe.targets.enabled", "true")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false").execute();
        assertThat(List.of(execution.testEvents().started().count(), execution.allEvents().skipped().count(),
                execution.allEvents().failed().count(), entries(execution).size()))
                .as("Обычный движок не должен запускать пробы или приобретать ресурс расширения")
                .isEqualTo(List.of(0L, 1L, 0L, 0));
    }

    @Test
    @DisplayName("ProbeEngine не запускает пробы без локального разрешения")
    void probeEngineCannotRunSuiteProbesWithoutPermission() {
        EngineExecutionResults execution = EngineTestKit.engine(new ProbeEngine()).selectors(selectClass(FirstProbe.class))
                .configurationParameter("rfr.probe.targets.enabled", "false")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false").execute();
        assertThat(List.of(execution.testEvents().started().count(), execution.allEvents().skipped().count(),
                execution.allEvents().failed().count(), entries(execution).size()))
                .as("Движок проб не должен запускать мишень или приобретать ресурс без локального разрешения")
                .isEqualTo(List.of(0L, 1L, 0L, 0));
    }

    private EngineExecutionResults execution(Map<String, String> configuration, Class<?>... targets) {
        var builder = EngineTestKit.engine(new ProbeEngine()).selectors(Arrays.stream(targets)
                        .map(target -> selectClass(target)).toArray(DiscoverySelector[]::new))
                .configurationParameter("rfr.probe.targets.enabled", "true")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$ClassName")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", "3")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.max-pool-size", "3")
                .configurationParameter("rfr.suite.probe.before.failure", "false")
                .configurationParameter("rfr.suite.probe.after.failure", "false");
        configuration.forEach(builder::configurationParameter);
        return builder.execute();
    }

    private List<Map<String, String>> entries(EngineExecutionResults execution) {
        return execution.allEvents().reportingEntryPublished()
                .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
                .filter(entry -> entry.containsKey("suite-probe")).toList();
    }

    private List<String> stages(EngineExecutionResults execution, String suite) {
        return entries(execution).stream().filter(entry -> entry.get("suite-probe").equals(suite))
                .map(entry -> entry.get("stage")).toList();
    }

    private List<String> owners(EngineExecutionResults execution, String suite) {
        return entries(execution).stream().filter(entry -> entry.get("suite-probe").equals(suite))
                .filter(entry -> !entry.get("stage").startsWith("body")).map(entry -> entry.get("owner")).toList();
    }

    private List<String> failures(EngineExecutionResults execution) {
        return execution.allEvents().failed().map(event -> event.getRequiredPayload(TestExecutionResult.class)
                .getThrowable().orElseThrow()).flatMap(this::messages).toList();
    }

    private Stream<String> messages(Throwable failure) {
        return Stream.concat(Stream.of(Optional.ofNullable(failure.getMessage()).orElse("")),
                Stream.concat(Optional.ofNullable(failure.getCause()).stream().flatMap(this::messages),
                        Arrays.stream(failure.getSuppressed()).flatMap(this::messages)));
    }

    /**
     * Наблюдает только публичные хуки SuiteExtension, включая частичное получение ресурса.
     */
    public static final class ObservedSuite implements SuiteExtension {
        private final AtomicReference<Optional<ProbeResource>> resource = new AtomicReference<>(Optional.empty());

        @Override
        public void beforeSuite(ExtensionContext context) {
            ProbeResource fixture = new ProbeResource(context, "primary", UUID.randomUUID().toString());
            resource.set(Optional.of(fixture));
            fixture.event("before");
            fixture.event("resource-acquired");
            if (fixture.configured("rfr.suite.probe.before.failure")) {
                fixture.event("before-error");
                throw new IllegalStateException("Намеренная ошибка beforeSuite пробы");
            }
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            try (ProbeResource fixture = resource.get().orElseThrow()) {
                fixture.event("after");
                if (fixture.configured("rfr.suite.probe.after.failure")) {
                    fixture.event("after-error");
                    throw new IllegalStateException("Намеренная ошибка afterSuite пробы");
                }
            }
        }
    }

    /**
     * Даёт независимого владельца ресурса для проверки закрытия остальных расширений.
     */
    public static final class CompanionSuite implements SuiteExtension {
        private final AtomicReference<Optional<ProbeResource>> resource = new AtomicReference<>(Optional.empty());

        @Override
        public void beforeSuite(ExtensionContext context) {
            ProbeResource fixture = new ProbeResource(context, "companion", UUID.randomUUID().toString());
            resource.set(Optional.of(fixture));
            fixture.event("before");
            fixture.event("resource-acquired");
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            try (ProbeResource fixture = resource.get().orElseThrow()) {
                fixture.event("after");
            }
        }
    }

    /**
     * Публикует наблюдаемое приобретение и освобождение собственного ресурса пробы.
     */
    private static final class ProbeResource implements AutoCloseable {
        private final ExtensionContext context;
        private final String suite;
        private final String owner;

        private ProbeResource(ExtensionContext context, String suite, String owner) {
            this.context = context;
            this.suite = suite;
            this.owner = owner;
        }

        private void event(String stage) {
            context.publishReportEntry(Map.of("suite-probe", suite, "stage", stage, "owner", owner));
        }

        private boolean configured(String name) {
            return context.getConfigurationParameter(name).map(Boolean::parseBoolean).orElse(false);
        }

        @Override
        public void close() {
            event("resource-released");
        }
    }

    /**
     * Предоставляет телу пробы журнал и ограниченную по времени точку синхронизации.
     */
    interface ProbeBody {
        void complete();

        void overlap() throws Exception;
    }

    /**
     * Передаёт контекст только явно запрошенному параметру пробы без подготовки данных.
     */
    public static final class ScenarioContext implements ParameterResolver {
        @Override
        public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
            return parameter.getParameter().getType().equals(ProbeBody.class);
        }

        @Override
        public ProbeBody resolveParameter(ParameterContext parameter, ExtensionContext context) {
            return new Body(context);
        }
    }

    /**
     * Записывает события тела отдельно от хуков проверяемого расширения.
     */
    private record Body(ExtensionContext context) implements ProbeBody {
        @Override
        public void complete() {
            context.publishReportEntry(Map.of("suite-probe", "primary", "stage", "body",
                    "thread", Long.toString(Thread.currentThread().threadId())));
        }

        @Override
        public void overlap() throws Exception {
            context.publishReportEntry(Map.of("suite-probe", "primary", "stage", "body-start",
                    "thread", Long.toString(Thread.currentThread().threadId())));
            context.getRoot().getStore(ExtensionContext.Namespace.create(SuiteExtensionTest.class, "barrier"))
                    .computeIfAbsent("gate", key -> new CyclicBarrier(3), CyclicBarrier.class).await(5, TimeUnit.SECONDS);
            complete();
        }
    }

    /**
     * Создаёт два отдельных контекста теста в первом классе.
     */
    @ProbeTarget
    @ExtendWith({ObservedSuite.class, ScenarioContext.class})
    @DisplayName("Первый класс проб жизненного цикла")
    static final class FirstProbe {
        @RepeatedTest(2)
        @DisplayName("Повтор первого класса публикует выполнение тела")
        void firstClassCannotRunWithoutSuite(ProbeBody body) {
            body.complete();
        }
    }

    /**
     * Создаёт ещё два контекста с новыми экземплярами того же типа расширения.
     */
    @ProbeTarget
    @ExtendWith({ObservedSuite.class, ScenarioContext.class})
    @DisplayName("Второй класс проб жизненного цикла")
    static final class SecondProbe {
        @RepeatedTest(2)
        @DisplayName("Повтор второго класса публикует выполнение тела")
        void secondClassCannotRunWithoutSuite(ProbeBody body) {
            body.complete();
        }
    }

    /**
     * Продлевает запуск после классов, на которых зарегистрировано проверяемое расширение.
     */
    @ProbeTarget
    @ExtendWith(ScenarioContext.class)
    @DisplayName("Последний класс без регистрации SuiteExtension")
    static final class UnextendedProbe {
        @Test
        @DisplayName("Класс без расширения завершает последнее тело запуска")
        void unregisteredClassCannotOutliveSuite(ProbeBody body) {
            body.complete();
        }
    }

    /**
     * Доходит до тела теста и намеренно завершается ошибкой.
     */
    @ProbeTarget
    @ExtendWith({ObservedSuite.class, ScenarioContext.class})
    @DisplayName("Проба падения тела теста")
    static final class FailingBodyProbe {
        @Test
        @DisplayName("Тело намеренно падает после публикации события")
        void failureCannotBypassSuiteCleanup(ProbeBody body) {
            body.complete();
            throw new AssertionError("Намеренное падение тела пробы");
        }
    }

    /**
     * Подключает два независимых типа расширений к одним тестам.
     */
    @ProbeTarget
    @ExtendWith({ObservedSuite.class, CompanionSuite.class, ScenarioContext.class})
    @DisplayName("Проба двух независимых расширений запуска")
    static final class CompanionProbe {
        @RepeatedTest(2)
        @DisplayName("Тело выполняется при двух подключённых расширениях")
        void companionCannotReplacePrimarySuite(ProbeBody body) {
            body.complete();
        }
    }

    /**
     * Меняет порядок регистрации, не закрепляя внутренний порядок закрытия ресурсов.
     */
    @ProbeTarget
    @ExtendWith({CompanionSuite.class, ObservedSuite.class, ScenarioContext.class})
    @DisplayName("Проба обратного порядка двух расширений запуска")
    static final class CompanionFirstProbe {
        @RepeatedTest(2)
        @DisplayName("Тело выполняется после регистрации расширений в обратном порядке")
        void registrationOrderCannotCancelCompanionClosure(ProbeBody body) {
            body.complete();
        }
    }

    /**
     * Ждёт одновременного входа трёх классов без зависимости от порядка планирования.
     */
    @ProbeTarget
    @ExtendWith({ObservedSuite.class, ScenarioContext.class})
    @DisplayName("Первый параллельный класс проб")
    static final class ParallelFirstProbe {
        @Test
        @DisplayName("Первый класс пересекается с двумя другими")
        void firstClassCannotFinishBeforeOthersStart(ProbeBody body) throws Exception {
            body.overlap();
        }
    }

    /**
     * Занимает второй поток до одновременного входа остальных классов.
     */
    @ProbeTarget
    @ExtendWith({ObservedSuite.class, ScenarioContext.class})
    @DisplayName("Второй параллельный класс проб")
    static final class ParallelSecondProbe {
        @Test
        @DisplayName("Второй класс пересекается с двумя другими")
        void secondClassCannotFinishBeforeOthersStart(ProbeBody body) throws Exception {
            body.overlap();
        }
    }

    /**
     * Освобождает барьер только после входа всех трёх классов.
     */
    @ProbeTarget
    @ExtendWith({ObservedSuite.class, ScenarioContext.class})
    @DisplayName("Третий параллельный класс проб")
    static final class ParallelThirdProbe {
        @Test
        @DisplayName("Третий класс пересекается с двумя другими")
        void thirdClassCannotFinishBeforeOthersStart(ProbeBody body) throws Exception {
            body.overlap();
        }
    }
}
