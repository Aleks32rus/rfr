package io.student.rangiffler.junit.probe;

import com.sun.net.httpserver.HttpServer;
import io.student.rangiffler.junit.annotation.ProbeTarget;
import io.student.rangiffler.junit.extension.StandReadinessExtension;
import io.student.rangiffler.stand.HttpService;
import io.student.rangiffler.stand.Stand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Проверяет запуск мишеней с настоящей проверкой готовности по HTTP.
 */
@Timeout(20)
@DisplayName("Проверка готовности стенда перед запуском тестов")
final class StandReadinessExtensionTest {
    @Test
    @DisplayName("Тест выполняется после того, как сервис начал отвечать")
    void testsRunWhenServiceRespondsLater() throws IOException {
        AtomicLong first = new AtomicLong();
        try (Stub stub = new Stub(() -> {
            long now = System.nanoTime();
            first.compareAndSet(0, now);
            return now - first.get() >= Duration.ofSeconds(2).toNanos();
        })) {
            assertThat(execution(Guarded.class, stub, true, Duration.ofSeconds(10)).testEvents().succeeded().count())
                    .as("Тест мишени не должен пропускаться после ответа сервиса")
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Тест не стартует после истечения времени ожидания")
    void testsDontRunWhenServiceStaysSilent() throws IOException {
        try (Stub stub = new Stub(() -> false)) {
            assertThat(execution(Guarded.class, stub, true, Duration.ofMillis(700)).testEvents().started().count())
                    .as("Тест мишени не должен стартовать, пока сервис недоступен")
                    .isZero();
        }
    }

    @Test
    @DisplayName("Ошибка контейнера содержит имя недоступного сервиса")
    void failureNamesSilentService() throws IOException {
        try (Stub stub = new Stub(() -> false)) {
            assertThat(execution(Guarded.class, stub, true, Duration.ofMillis(700)).containerEvents().failed()
                    .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow())
                    .map(Throwable::getMessage))
                    .as("Ошибка контейнера не должна скрывать имя недоступного сервиса")
                    .anyMatch(message -> message.contains("rfr-stub-7319"));
        }
    }

    @Test
    @DisplayName("Выключенная проверка не обращается к сервису")
    void disabledCheckSendsNoRequests() throws IOException {
        try (Stub stub = new Stub(() -> true)) {
            execution(Guarded.class, stub, false, Duration.ofSeconds(3));
            assertThat(stub.requests.get())
                    .as("Выключенная проверка не должна отправлять запросы")
                    .isZero();
        }
    }

    @Test
    @DisplayName("Класс без тега не обращается к сервису")
    void untaggedClassSendsNoRequests() throws IOException {
        try (Stub stub = new Stub(() -> true)) {
            execution(Untagged.class, stub, true, Duration.ofSeconds(3));
            assertThat(stub.requests.get())
                    .as("Мишень без тега не должна отправлять запросы")
                    .isZero();
        }
    }

    private EngineExecutionResults execution(Class<?> target, Stub stub, boolean check, Duration timeout) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(target))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("rfr.probe.targets.enabled", "true")
                .configurationParameter("rfr.probe.stand.uri", stub.uri())
                .configurationParameter("rfr.probe.stand.check", Boolean.toString(check))
                .configurationParameter("rfr.probe.stand.timeout", timeout.toString())
                .execute();
    }

    /**
     * Передаёт параметры пробы настоящему расширению проверки готовности.
     */
    static final class StubbedStand implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            URI uri = URI.create(context.getConfigurationParameter("rfr.probe.stand.uri").orElseThrow());
            boolean check = Boolean.parseBoolean(context.getConfigurationParameter("rfr.probe.stand.check").orElseThrow());
            Duration timeout = Duration.parse(context.getConfigurationParameter("rfr.probe.stand.timeout").orElseThrow());
            new StandReadinessExtension(check, "probe-stand",
                    new Stand(List.of(new HttpService("rfr-stub-7319", "start the stub", uri)),
                            timeout, Duration.ofMillis(100))).beforeAll(context);
        }
    }

    /**
     * Наблюдает запуск теста с проверкой готовности.
     */
    @ProbeTarget
    @ExtendWith(StubbedStand.class)
    @Tag("probe-stand")
    @DisplayName("Мишень с проверкой готовности")
    static final class Guarded {
        @Test
        @DisplayName("Выполнение теста мишени с тегом")
        void completes() {
        }
    }

    /**
     * Наблюдает запуск теста без тега проверки готовности.
     */
    @ProbeTarget
    @ExtendWith(StubbedStand.class)
    @DisplayName("Мишень без тега проверки готовности")
    static final class Untagged {
        @Test
        @DisplayName("Выполнение теста мишени без тега")
        void completes() {
        }
    }

    /**
     * Владеет локальным HTTP-сервером и фиксирует обращения к нему.
     */
    private static final class Stub implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger requests = new AtomicInteger();

        private Stub(BooleanSupplier ready) throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", exchange -> {
                try (exchange) {
                    requests.incrementAndGet();
                    exchange.sendResponseHeaders(ready.getAsBoolean() ? 200 : 503, -1);
                }
            });
            server.start();
        }

        private String uri() {
            return "http://localhost:" + server.getAddress().getPort() + "/";
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
