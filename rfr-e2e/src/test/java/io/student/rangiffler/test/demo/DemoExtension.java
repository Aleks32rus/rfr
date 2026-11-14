package io.student.rangiffler.test.demo;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.PreInterruptCallback;
import org.junit.jupiter.api.extension.PreInterruptContext;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

import java.util.Objects;

/**
 * Показывает вызовы расширения Jupiter без ресурсов приложения и общего состояния.
 * Создание и вызовы передаются в общее дерево жизненного цикла с идентификаторами экземпляров.
 * Callbacks шаблонов класса и прерывания реализованы, но обычные тесты их не вызывают.
 */
public final class DemoExtension implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback,
        TestInstancePreConstructCallback, TestInstancePostProcessor, TestInstancePreDestroyCallback,
        BeforeClassTemplateInvocationCallback, AfterClassTemplateInvocationCallback,
        PreInterruptCallback, ParameterResolver {
    private final long creation;

    public DemoExtension() {
        this.creation = System.nanoTime();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        report("beforeAll", context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        report("afterAll", context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        report("beforeEach", context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        report("afterEach", context);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        report("beforeTestExecution", context);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        report("afterTestExecution", context);
    }

    @Override
    public void preConstructTestInstance(TestInstanceFactoryContext factory, ExtensionContext context) {
        report("preConstructTestInstance", context);
    }

    @Override
    public void postProcessTestInstance(Object instance, ExtensionContext context) {
        report("postProcessTestInstance", context);
    }

    @Override
    public void preDestroyTestInstance(ExtensionContext context) {
        report("preDestroyTestInstance", context);
    }

    @Override
    public void beforeClassTemplateInvocation(ExtensionContext context) {
        report("beforeClassTemplateInvocation", context);
    }

    @Override
    public void afterClassTemplateInvocation(ExtensionContext context) {
        report("afterClassTemplateInvocation", context);
    }

    @Override
    public void beforeThreadInterrupt(PreInterruptContext interruption, ExtensionContext context) {
        report("beforeThreadInterrupt", context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        report("supportsParameter[" + parameter.getIndex() + "]", context);
        return parameter.isAnnotated(DemoParameter.class);
    }

    @Override
    public String resolveParameter(ParameterContext parameter, ExtensionContext context) {
        report("resolveParameter", context);
        return "Демонстрационный параметр";
    }

    private void report(String event, ExtensionContext context) {
        Objects.requireNonNull(context.getStore(ExtensionContext.Namespace.create(DemoTrace.class))
                        .get(DemoTrace.class, DemoTrace.class), "Не найден журнал жизненного цикла демо")
                .record(this, creation, event, context);
    }
}
