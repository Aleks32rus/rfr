package io.student.rangiffler.junit.engine;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import java.util.ServiceLoader;

/**
 * Запускает мишени через Jupiter с отдельным идентификатором контекста движка.
 */
public final class ProbeEngine implements TestEngine {
    private final TestEngine origin;

    public ProbeEngine() {
        this(ServiceLoader.load(TestEngine.class).stream().map(ServiceLoader.Provider::get)
                .filter(engine -> engine.getId().equals("junit-jupiter")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Движок Jupiter недоступен для запуска мишеней")));
    }

    private ProbeEngine(TestEngine origin) {
        this.origin = origin;
    }

    @Override
    public String getId() {
        return "rfr-probes";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId id) {
        return origin.discover(request, id);
    }

    @Override
    public void execute(ExecutionRequest request) {
        origin.execute(request);
    }
}
